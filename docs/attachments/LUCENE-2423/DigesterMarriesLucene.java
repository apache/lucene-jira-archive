import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;


/**
 * Parses the contents of address-book XML file and indexes all
 * contact entries found in it.  The name of the file to parse must be
 * specified as the first command line argument.
 */
public class DigesterMarriesLucene
{
    private static IndexWriter writer;

    /**
     * Adds the contact to the index.
     *
     * @param contact the <code>Contact</code> to add to the index
     */
    public void addContact(Contact contact) throws IOException
    {
        System.out.println("Adding " + contact.getName());
        Document contactDocument  = new Document();
        contactDocument.add(new Field("type", contact.getType(), Field.Store.YES, Field.Index.ANALYZED));
        contactDocument.add(new Field("name", contact.getName(), Field.Store.YES, Field.Index.ANALYZED));
        contactDocument.add(new Field("address", contact.getAddress(), Field.Store.YES, Field.Index.ANALYZED));
        contactDocument.add(new Field("city", contact.getCity(), Field.Store.YES, Field.Index.ANALYZED));
        contactDocument.add(new Field("province", contact.getProvince(), Field.Store.YES, Field.Index.ANALYZED));
        contactDocument.add(new Field("postalcode", contact.getPostalcode(), Field.Store.YES, Field.Index.ANALYZED));
        contactDocument.add(new Field("country", contact.getCountry(), Field.Store.YES, Field.Index.ANALYZED));
        contactDocument.add(new Field("telephone", contact.getTelephone(), Field.Store.YES, Field.Index.ANALYZED));

	System.out.println("Added Telephone to contactDocument: " + contact.getTelephone());
	System.out.println("Printed Telephone data from contactDocument: " + contactDocument.getFields("telephone"));
	System.out.println("Printed entire contactDocument: " + contactDocument.getFields());
	System.out.println("Within addContact(), Print name of Directory used by writer: " + writer.getDirectory());
	System.out.println("Statement in addContact() just prior to 'writer.adddocument', number of documents in contactDocument: " + writer.numDocs());

        writer.addDocument(contactDocument);
    }

    /**
     * Created an index to add contacts to, configures Digester rules and
     * actions, parses the XML file specified as the first argument.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) throws IOException, SAXException
    {
        String indexDir =
            System.getProperty("java.io.tmpdir", "tmp") +
            System.getProperty("file.separator") + "address-book";
	System.out.println("From first line in main() print value of indexDir: " + indexDir);

        Analyzer analyzer = new WhitespaceAnalyzer();
        boolean createFlag = true;

        // IndexWriter to use for adding contacts to the index
	System.out.println("5th statement in main creates an instance of Indexwriter named 'writer'");
	final File INDEX_DIR = new File(indexDir);
        IndexWriter writer = new IndexWriter(FSDirectory.open(INDEX_DIR), analyzer, createFlag, IndexWriter.MaxFieldLength.LIMITED);
	System.out.println("8th statement in main(), Prints name of Directory used by writer: " + writer.getDirectory());
	System.out.println("9th Statement in main(), number of documents in contactDocument: " + writer.numDocs());
	System.out.println("At this point the files in this directory are empty");

	//IndexWriter writer = new IndexWriter(indexDir, analyzer, createFlag, IndexWriter.MaxFieldLength.UNLIMITED);

	// instantiate Digester and disable XML validation
        Digester digester = new Digester();
        digester.setValidating(false);

        // instantiate DigesterMarriesLucene class
        digester.addObjectCreate("address-book", DigesterMarriesLucene.class );
        // instantiate Contact class
        digester.addObjectCreate("address-book/contact", Contact.class );

        // set type property of Contact instance when 'type' attribute is found
        digester.addSetProperties("address-book/contact",         "type", "type" );

        // set different properties of Contact instance using specified methods
        digester.addCallMethod("address-book/contact/name",       "setName", 0);
        digester.addCallMethod("address-book/contact/address",    "setAddress", 0);
        digester.addCallMethod("address-book/contact/city",       "setCity", 0);
        digester.addCallMethod("address-book/contact/province",   "setProvince", 0);
        digester.addCallMethod("address-book/contact/postalcode", "setPostalcode", 0);
        digester.addCallMethod("address-book/contact/country",    "setCountry", 0);
        digester.addCallMethod("address-book/contact/telephone",  "setTelephone", 0);

        // call 'addContact' method when the next 'address-book/contact' pattern is seen
        digester.addSetNext("address-book/contact",               "addContact" );

	// now that rules and actions are configured, start the parsing process
        DigesterMarriesLucene dml = (DigesterMarriesLucene) digester.parse(new File(args[0]));

        // optimize and close the index
        writer.optimize();
        writer.close();
    }

    /**
     * JavaBean class that holds properties of each Contact entry.
     * It is important that this class be public and static, in order for
     * Digester to be able to instantiate it.
     */
    public static class Contact
    {
        private String type;
        private String name;
        private String address;
        private String city;
        private String province;
        private String postalcode;
        private String country;
        private String telephone;

        public void setType(String newType)
        {
            type = newType;
        }
        public String getType()
        {
            return type;
        }

        public void setName(String newName)
        {
            name = newName;
        }
        public String getName()
        {
            return name;
        }

        public void setAddress(String newAddress)
        {
            address = newAddress;
        }
        public String getAddress()
        {
            return address;
        }

        public void setCity(String newCity)
        {
            city = newCity;
        }
        public String getCity()
        {
            return city;
        }

        public void setProvince(String newProvince)
        {
            province = newProvince;
        }
        public String getProvince()
        {
            return province;
        }

        public void setPostalcode(String newPostalcode)
        {
            postalcode = newPostalcode;
        }
        public String getPostalcode()
        {
            return postalcode;
        }

        public void setCountry(String newCountry)
        {
            country = newCountry;
        }
        public String getCountry()
        {
            return country;
        }

        public void setTelephone(String newTelephone)
        {
            telephone = newTelephone;
        }
        public String getTelephone()
        {
            return telephone;
        }
    }
}
