# transmart-horde
This contains code for a prototype of "federation"
It only considers the concept tree so far. Data is going to be hard.

Configuration to add in Config.groovy

transmart { horde {

	uuid = "7d6ff94b-363f-9c3a-8a09-0ded5728b436"
	endpoints = [
		"http://742.34.4.178/transmart",
		"http://example.com/transmart"
	]
}}

Add to Spring Security chain :

'/horde/**'                   : ["permitAll"],
'/hordeConcepts/**'           : ["permitAll"],

No tests are yet available for the Services