import math
# title count=5000000 nullCount=0 totLen=125382377 utf8Len=125732531

# actual numbers from 5M doc wikipedia index:
count = 5000000
totLen = 125382377
utf8Len = 125732531

OBJ_OVERHEAD = 8

INT = 4

ARR_OVERHEAD = OBJ_OVERHEAD+2*INT

CHAR = 2

POINTER = 4

# String.java has 4 fields: char[] value, int offset, int count, int hash
STRING_OVERHEAD = OBJ_OVERHEAD + POINTER + ARR_OVERHEAD + 3*INT

print 'STORE ONLY'
mb1 = ((count * (POINTER + STRING_OVERHEAD) + CHAR * totLen)/1024./1024.)
print '  32 bit current: %5.1f MB' % mb1
      

bitsPerOffset = int(math.ceil(math.log(utf8Len)/math.log(2.0)))
mb2 = ((utf8Len + count * bitsPerOffset / 8)/1024./1024.)
print '  32 bit     new: %5.1f MB [%.1f%% smaller]' % (mb2, 100.0*(mb1-mb2)/mb1)

POINTER = 8

STRING_OVERHEAD = OBJ_OVERHEAD + POINTER + ARR_OVERHEAD + 3*INT

mb1 = ((count * (POINTER + STRING_OVERHEAD) + CHAR * totLen)/1024./1024.)
print '  64 bit current: %5.1f MB' % mb1

mb2 = ((utf8Len + count * bitsPerOffset / 8)/1024./1024.)      
print '  64 bit     new: %5.1f MB [%.1f%% smaller]' % (mb2, 100.0*(mb1-mb2)/mb1)
      



POINTER = 4

# String.java has 4 fields: char[] value, int offset, int count, int hash
STRING_OVERHEAD = OBJ_OVERHEAD + POINTER + ARR_OVERHEAD + 3*INT

print 'SORTING'

mb1 = ((count * (INT + POINTER + STRING_OVERHEAD) + CHAR * totLen)/1024./1024.)
print '  32 bit current: %5.1f MB' % mb1
      

bitsPerOrd = int(math.ceil(math.log(count)/math.log(2.0)))
mb2 = ((utf8Len + count * (bitsPerOffset+bitsPerOrd) / 8)/1024./1024.)
print '  32 bit     new: %5.1f MB [%.1f%% smaller]' % (mb2, 100.0*(mb1-mb2)/mb1)
      


POINTER = 8

STRING_OVERHEAD = OBJ_OVERHEAD + POINTER + ARR_OVERHEAD + 3*INT
mb1 = ((count * (INT + POINTER + STRING_OVERHEAD) + CHAR * totLen)/1024./1024.)
print '  64 bit current: %5.1f MB' % mb1
      

bitsPerOrd = int(math.ceil(math.log(count)/math.log(2.0)))
mb2 = ((utf8Len + count * (bitsPerOffset+bitsPerOrd) / 8)/1024./1024.)
print '  64 bit     new: %5.1f MB [%.1f%% smaller]' % (mb2, 100.0*(mb1-mb2)/mb1)
      
