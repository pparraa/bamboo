## release 1.0.0 (September 21, 2018)
- upgraded to Atlassian SDK 6.3.10 (supports Bamboo 5.10.0 - 6.2.1)
- several 'under-the-hood' improvements, including:
    - build log now echo-ing to the Bamboo server log, if debug is on
    - clean-up of the pom file
    - new class name for debug logging (com.veracode.bamboo.uploadscan)

## release 0.0.1f (January 5, 2018)
- upgraded to the open source version of the Veracode API library
- hide the 'Bus Crit' selection unless 'Create New' is checked
- fixed some error reporting where info was going to the global Bamboo log instead of the job log
- (internal) re-factoring Veracode credentials handling into a common class

## release 0.0.1e (December 7, 2017)
Tested on Bamboo 6.2.3 (Windows) as well as 5.15 on Mac
- fail the job if no files are found to upload
- handle Windows backslash in pathnames for file matching (escape the backslash before attempting the glob-style match)

## release 0.0.1d (December 1, 2017)
Added a custom global variable, 'com.veracode.hideWait' that, when set to true, will hide the 'Wait For Scan to Complete' checkbox.

## release 0.0.1c (November 28, 2017)
Added masking of password and API-key fields.

## release 0.0.1b (October 27, 2017)
Tested with Bamboo 5.15.0.1  
Beta release.  Fully functional, but with limited testing.

## release 0.0.1a (September 28, 2017)
Tested with Bamboo 5.15.0.1  
What works:
- find existing or create new application
- find existing or create new sandbox
- file upload include and exclude lists
- selectable credentials, username/password or API ID/KEY
- help text for all the supported features

What's missing:
- module selection (scan include/exclude)
- file renaming
- wait for scan to complete
- encrypting credentials
- help text for all the unsupported features
- proxy support (although this might be handled by the Bamboo global proxy settings - needs further investigation)
