# Publisher

The Publisher component of the IES takes on the task of publishing data by compiling data from a resource with the help of aggregators and writing it as a file to the file system.

These files are made available to the web server, with which these resources can then be further processed for the website.

Other functions that are also assigned to this Publisher component are linked to the publication process.

## Link Checker

For an aggregated resource, all external links are collected and stored in the database. A scheduler checks the validity of the links at regular intervals and updates the status in the database.
The status can be determined via the CMS to give the editors the opportunity to correct the links.

## UseCases
