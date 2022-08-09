import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class SecureLucene
{
    private static  final   String  LUCENE_DIR = "luceneTest";
    private static  final   String  POLICY_FILE = "luceneTest.policy";
    private static  final   String  FILE_URL_TOKEN = "$fileURLToken";
    private static  final   String  FILE_TOKEN = "$fileToken";
    private static  final   String  LUCENE_JAR_FILE_TOKEN = "$luceneJarFileToken";

    private static  final   String  LUCENE_PRIVS =
        "grant codeBase \"" + FILE_URL_TOKEN + "\"\n" +
        "{\n" +
        "  // permissions for file access, write access only to sandbox:\n" +
        "  permission java.io.FilePermission \"" + FILE_TOKEN + "\", \"read,write,delete\";\n" +
        "  permission java.io.FilePermission \"" + FILE_TOKEN + "/-\", \"read,write,delete\";\n" +
        "  \n" +
        "  // Basic permissions needed for Lucene to work:\n" +
        "  permission java.util.PropertyPermission \"user.dir\", \"read\";\n" +
        "  permission java.util.PropertyPermission \"sun.arch.data.model\", \"read\";\n" +
        "};\n";

    private static  final   String  APP_PRIVS =
        "grant codeBase \"" + FILE_URL_TOKEN + "\"\n" +
        "{\n" +
        "  // permissions for file access, write access only to sandbox:\n" +
        //"  permission java.io.FilePermission \"<<ALL FILES>>\", \"read\";\n" +
        "  permission java.io.FilePermission \"" + LUCENE_JAR_FILE_TOKEN + "\", \"read\";\n" +
        "  permission java.io.FilePermission \"" + FILE_TOKEN + "\", \"read,write\";\n" +
        "  permission java.io.FilePermission \"" + FILE_TOKEN + "/-\", \"read,write,delete\";\n" +
        "  \n" +
        "  // Basic permissions needed for Lucene to work:\n" +
        "  permission java.util.PropertyPermission \"user.dir\", \"read\";\n" +
        "  permission java.util.PropertyPermission \"sun.arch.data.model\", \"read\";\n" +
        "};\n";

    /**
     * Takes one arg: "true" means run with a security manager and "false" means don't.
     * If the argument is omitted, runs first without a security manager and then again with one.
     *
     */
    public  static  void    main( String... args ) throws Exception
    {
        if ( (args != null) && (args.length > 0) )
        {
            runTest( Boolean.valueOf( args[ 0 ] ).booleanValue() );
        }
        else
        {
            runTest( false );
            runTest( true );
        }
    }

    private static  void    runTest( boolean installSecurityManager )
        throws Exception
    {
        if ( installSecurityManager ) { System.out.println( "Running with security manager..." ); }
        else { System.out.println( "Running unsecured..." ); }
        
        File    directory = new File( LUCENE_DIR );
        deleteFile( directory );

        if ( installSecurityManager ) { installSecurityManager( directory ); }

        IndexWriter iw = getIndexWriter( directory );
        Document doc = new Document();

        doc.add( new TextField( "foo", "bar", Store.NO) );
        iw.addDocument( doc );
        iw.close();

        System.out.println( "    Success!" );
    }

    private static  void    installSecurityManager( File outputDir )
        throws Exception
    {
        File    policyFile = new File( POLICY_FILE );
        writePolicyFile( outputDir, policyFile );

        System.setProperty( "java.security.policy", policyFile.getAbsolutePath() );
        System.setSecurityManager( new SecurityManager() );
    }

    private static  void    writePolicyFile( File outputDir, File policyFile )
        throws Exception
    {
        URL luceneCore = getURL( "org.apache.lucene.store.FSDirectory" );
        String  luceneCoreJarfileName = luceneCore.getFile();
        URL application = getURL( "SecureLucene" );
        String  luceneBlock = codeBlockPermissions( LUCENE_PRIVS, outputDir, luceneCore, luceneCoreJarfileName );
        String  applicationBlock = codeBlockPermissions( APP_PRIVS, outputDir, application, luceneCoreJarfileName );
        PrintStream ps = new PrintStream( policyFile );

        ps.println( luceneBlock );
        ps.println( applicationBlock );
        ps.close();
    }

    private static  String  codeBlockPermissions
        (
         String privTemplate,
         File outputDir,
         URL codeBlock,
         String luceneCoreJarfileName
         )
        throws Exception
    {
        String  policyContents = privTemplate.replace
            ( FILE_URL_TOKEN, codeBlock.toExternalForm() ).replace
            ( FILE_TOKEN, outputDir.getAbsolutePath()).replace
            ( LUCENE_JAR_FILE_TOKEN, luceneCoreJarfileName );

        return policyContents;
    }
    
	private static IndexWriter getIndexWriter( final File directory )
        throws IOException, PrivilegedActionException
    {
        return AccessController.doPrivileged
            (
             new PrivilegedExceptionAction<IndexWriter>()
             {
                 public IndexWriter run() throws IOException
                 {
                     Directory dir = FSDirectory.open( directory );

                     // allow this to be overridden in the configuration during load later.
                     Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_45);
                     IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_45,
                                                                   analyzer);
                     IndexWriter iw = new IndexWriter(dir, iwc);
		
                     return iw;
                 }
             }
             );
	}

    /**
     * Delete a file. If it's a directory, recursively delete all directories
     * and files underneath it first.
     */
    static  boolean deleteFile( File file )
        throws IOException, PrivilegedActionException
    {
        boolean retval = true;
        
        if ( isDirectory( file ) )
        {
            for ( File child : listFiles( file, null ) ) { retval = retval && deleteFile( child ); }
        }

        return retval && clobberFile( file );
    }

    /** Return true if the file is a directory */
    static  boolean isDirectory( final File file )
        throws IOException, PrivilegedActionException
    {
        return AccessController.doPrivileged
            (
             new PrivilegedExceptionAction<Boolean>()
             {
                public Boolean run() throws IOException
                {
                    return file.isDirectory();
                }
             }
             ).booleanValue();
    }

    /** Really delete a file */
    private static  boolean clobberFile( final File file )
        throws IOException, PrivilegedActionException
    {
        return AccessController.doPrivileged
            (
             new PrivilegedExceptionAction<Boolean>()
             {
                public Boolean run() throws IOException
                {
                    return file.delete();
                }
             }
             ).booleanValue();
    }

    private static  File[]  listFiles( final File file, final FileFilter fileFilter )
        throws IOException, PrivilegedActionException
    {
        return AccessController.doPrivileged
            (
             new PrivilegedExceptionAction<File[]>()
             {
                public File[] run() throws IOException
                {
                    if ( fileFilter == null )   { return file.listFiles(); }
                    else { return file.listFiles( fileFilter ); }
                }
             }
             );
    }

    /**
     * Get the URL of the code base from a class name.
     * If the class cannot be loaded, null is returned.
     */
    private static URL getURL(String className) {
        try {
            return getURL(Class.forName(className));
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }
	
	/**
	 * Get the URL of the code base from a class.
	 */
    @SuppressWarnings("rawtypes")
	private static URL getURL(final Class cl)
	{
        return AccessController.doPrivileged(new PrivilegedAction<URL>() {

			public URL run() {

                /* It's possible that the class does not have a "codeSource"
                 * associated with it (ex. if it is embedded within the JVM,
                 * as can happen with Xalan and/or a JAXP parser), so in that
                 * case we just return null.
                 */
                if (cl.getProtectionDomain().getCodeSource() == null)
                    return null;

				return cl.getProtectionDomain().getCodeSource().getLocation();
			}
		});
	}


}
