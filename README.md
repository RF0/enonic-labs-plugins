Enonic Labs plugins

#enonic-labs-plugins
This repository is organized with a parent pom that contains sub projects that compiles into separate jar files.

##Building

Build all code and create Enonic CMS deployable jar file with
    mvn clean install

##datarefine-plugin
Plugin to refine messy data. Clean up dataset, mass edit in Enonic CMS Admin.
urlPatterns : /admin/site/[0-9]/datarefine-plugin.*

Status: FINISHED

Tested on Enonic CMS 4.7.x

Read more on:
http://labs.enonic.com/articles/datarefine-plugin

##esocial-plugin
Facebook login plugin.
urlPatterns : /site/[0,5]/facebook-signin.*

Status: BETA.
Ready for testing. Login implemented, creating user in Enonic CMS Facebook userstore for new users.
Tested on Enonic CMS 4.7.x

##germ-plugin
G.E.R.M - Git Enonic Release Management - A plugin that connects Enonic CMS to Git, and allows to pull code directly from any
git remote into CMS_HOME resources and plugin folders.
urlPatterns : /admin/site/[0-9]/germ.*

Status: BETA.
Ready for testing, all core functionality in place: add remote, fetch, reset, status.
Tested on Enonic CMS 4.7.x

More on GERM here:
https://enonic.com/en/docs/enonic-cms-47?page=Development+Process+-+GERM

##License

This software is licensed under AGPL 3.0 license. Also the distribution includes 3rd party software components.
The vast majority of these libraries are licensed under Apache 2.0. For a complete list please read NOTICE.txt.

You are free to use, change and re-distribute the software according to license
The software is provided "AS IS" - Enonic accepts no liability, indemnity or warranties

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as
	published by the Free Software Foundation, either version 3 of the
	License, or (at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.
