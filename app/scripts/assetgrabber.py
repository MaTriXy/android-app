#!/usr/bin/env python

import os
import sys
import string
import shutil


def copyfile(filename, fullpath):
    global assetpath
    assetdir = fullpath[len(assetpath):]
    if assetdir.startswith('/_'):
        return
    outfile = '../res' + assetdir
    #outfile = outfile.replace('medium', 'middle')
    print 'cp \'' + fullpath + '\' \'' + outfile + '\''
    shutil.copyfile(fullpath, outfile.lower())

if __name__ == "__main__":
    global assetpath
    assetpath = '/Volumes/Team - UX/3. Design Direction/DESIGN/App artwork/_Android/_assets'
    if len(sys.argv) > 1:
        assetpath = sys.argv[1]
    for root, subFolders, files in os.walk(assetpath):
        for ffile in files:
            testfile = os.path.join(root,ffile)
            extension = os.path.splitext(testfile)[1]
            if extension and extension == '.png':
                copyfile(ffile, testfile)

