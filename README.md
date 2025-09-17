if you want this amazing tool running somewhere else than on a debian / ubuntu platform,
please checkout the code, run the build target packageRelease<platform>
and open a merge request.

====

json config file is under ~/.simplemoni/config.json

no worries: if not existing, it will be created for you on first run.

requires an array and items with the following properties:

active = boolean
weight = higher number means more important
interval = in seconds, how often to check if last check was ok
errorInterval = in seconds, how often to check if last check was not ok
requiredStatusCode if null = 200 - 300
description = description of the check
host = url to check, need full url

    {
        "name": "title of a card",
        "active": true,     
        "interval": 30,
        "errorInterval": 5,
        "weight": 100,
        requiredStatusCode: 401,
        "description": "my page",
        "host": "https://www.google.comfoo/"
    }