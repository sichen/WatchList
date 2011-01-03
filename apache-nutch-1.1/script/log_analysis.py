#!/usr/bin/env python
'''
A Python script to parse the Nutch hadoop.log file, and discover possible errors.

Created on Jan 1, 2011

@author: sichen
'''
import sys
import os
from urllib import urlopen
from optparse import OptionParser


def build_error_set(log_file):
    currentURL = None
    total_records = 0
    dict = {}
    try:
        f = open(log_file)

        while True:
            line = f.readline()
            if line.find("jcrew.JCrewParser - Start parsing:") > 0:
                # This is the start or a URL
                currentURL = line.split("jcrew.JCrewParser - Start parsing:")[1].strip()
                # Read the next line, see if it is the start of a JCrew product record
                line = f.readline()
                if line.find("jcrew.JCrewParser - Started parsing title for JCrew Product Item") > 0:
                    line = f.readline()
                    title = "Unknown"
                    while line.find("jcrew.JCrewParser - Finished parsing") < 0:
                        # process the record
                        if line.find("Got the productTitle for JCrew Product Item") > 0:
                            l = line.split(":")
                            m = len(l) -1
                            title = l[m].strip()
                            total_records += 1
                        if line.find("is null") > 0:
                            if dict.has_key(currentURL):
                                dict[currentURL].append(title)
                            else:
                                dict[currentURL] = []
                                dict[currentURL].append(title)
                            print ("found problem: " + currentURL)
                        line = f.readline()
            elif line == '':
                # nothing left
                break
            else:
                continue
    finally:
        f.close()
        print "totally found: " + str(total_records) + "records"
        print "totally found: " + str(len(dict)) + "problems"
        return dict


def main():
    parser = options_parser(sys.argv[0])
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 3:
        print parser.print_help()
        sys.exit(1)
    if os.path.exists(args[2]) and not os.path.isdir(args[2]):
        print args[2] + "is not a directory"
        sys.exit(1)
    
    processed = 0

    try:
        error_dict = build_error_set(args[1])
        print "finally, log analysis is finished."
        if options.no_testdata == True:
            print "Skip test data generation"
            return 0
        if not os.path.exists(args[2]):
            os.makedirs(args[2])
        pagelistFile = open(os.path.join(args[2], "pagelist.txt"), "w")
        javaFile = open(os.path.join(args[2], "test.java"), "w")
        for u, v in error_dict.iteritems():
            print "processing: " + u + ": " + v[0]
            # Now I got a list of the error URLs
            # 1. write the list to a file  
            pagelistFile.write(u)
            pagelistFile.write("\n")
            # 2. fetch those URLs and save them as 
            # test data 
            l = u.split('/')
            m = len(l) -1;
            contentFile = open(os.path.join(args[2], l[m]), "w")
            print "going to fetch: " + u
            urlFile = urlopen(u)
            for line in urlFile.readlines():
                contentFile.write(line)
            contentFile.close()
            # 3. generate java unit test code so 
            # I can easily copy paste into my code 
            testLine1 = 'pageTest(new File(testDir, "' + l[m] + '"), "' + v[0] + '",'
            testLine2 = '"' + u + '");'
            javaFile.write(testLine1)
            javaFile.write("\n")
            javaFile.write(testLine2)
            javaFile.write("\n")
            javaFile.write("\n")
            pagelistFile.flush()
            javaFile.flush() 
            processed += 1          
        pagelistFile.close()
        javaFile.close()

    except IOError, e:
        print "IOError: %s" % (str(e))
        sys.exit(1)
    finally:
        print "processed: " + str(processed)
        


def options_parser(scriptname):
    usage = "Usage: " + scriptname + " log_file_path test_data_dir"

    parser = OptionParser(usage)
    parser.add_option("-n", "--no-testdata-generation",
                  action="store_true", dest="no_testdata", default=False,
                  help="don't generate the test data")



    return parser


# The Main Function
if __name__ == "__main__":
    main()
