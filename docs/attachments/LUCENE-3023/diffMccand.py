import subprocess
import sys

#p = subprocess.Popen(['diff', '-rub', sys.argv[1], sys.argv[2]], shell=False, stdout=subprocess.PIPE)
#p = subprocess.Popen(['diff', '-rcB', sys.argv[1], sys.argv[2]], shell=False, stdout=subprocess.PIPE)
p = subprocess.Popen(['diff', '-ruN', sys.argv[1], sys.argv[2]], shell=False, stdout=subprocess.PIPE)

keep = False
while True:
 l = p.stdout.readline()
 if l == '':
   break
 if l.endswith('\r\n'):
   l = l[:-2]
 elif l.endswith('\n'):
   l = l[:-1]
 if l.startswith('diff '):
   keep = l.lower().find('/build/') == -1 and (l.lower().startswith('Only in') or ((l.lower().endswith('.java') or l.lower().endswith('.txt') or l.lower().endswith('.xml')) and l.find('/.svn/') == -1))
   if keep:
     print
     print
     print l
 elif keep:
   print l
 elif l.startswith('Only in'):
   print l
