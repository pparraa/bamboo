# What is this?

This is a Bamboo plugin that enables uploading files to Veracode for static scanning.  

# Upgrading

I recommend over-writing an existing copy with a newer copy of the plugin.  Follow the 'Installing' instructions below and just copy the new version over the older version.

If you decide to uninstall the previous version and install a new version you'll likely have to re-enter all of the passwords/API-keys as they are encrypted for storage.

# Installing

Please see the RELEASE_NOTES.md file for info on what's new.

Assuming you don't want to build this from scratch (see below for instructions), download the latest version from the releases directory and:
- From the 'Bamboo Administration' (Gear) menu select 'Add-ons'
- Select 'Upload Add-on'
- Choose the UploadScan-x.jar file you just downloaded

The Veracode plugin will now be available to add as a task in a job.  It will appear under both the 'All' and 'Tests' task types.

# Configuration

The plugin supports the following global variable(s):
- com.veracode.hideWait: when set to true the "Wait for Scan to Complete" checkbox will be hidden

# Building

Install the Atlassian SDK: https://developer.atlassian.com/docs/getting-started

Build with the 'atlas-run' command from the Atlassian SDK.  This will build the plugin and fire up a development instance of Bamboo with the plugin installed.  See the Atlassian docs for further info.

Note that the plugin is configured for debug logging that will generate additional information.  To configure this:
- From the 'Bamboo Administration' (Gear) menu select anything.  We just want to get to the Admin page.
- Towards the bottom of the left-hand pane click on 'Log settings'
- For the Classpath, enter 'com.veracode.bamboo.uploadscan' (no quotes)
- Leave the Type = debug
- Now there will be additional debug messages in the Bamboo log, which is typically [bamboo-home]/logs/atlassian-bamboo.log.

# Future plans

Things to include in future releases:
- check for existing scan and delete (checkbox)
- support for remote agents
- enhanced support for build results display

# Help with problems
Please log an issue.  At a minimum I'll need your build log and the information you put into each field for the task.

# A note about the author
While it's true that I work for Veracode, this is NOT an official Veracode-supported product.  I've written this in my own time in an effort to help support our customers.
