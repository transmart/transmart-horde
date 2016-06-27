# transmart-horde
This contains code for a prototype of "federation".

It only considers the concept tree so far. Data is going to be hard.

*No tests are yet available*

## Disclaimer

Do not use as such on internet-exposed machine !

## Configuration

Add as object to Config.groovy

```groovy
transmart { horde {

	uuid = "7d6ff94b-363f-9c3a-8a09-0ded5728b436"
	endpoints = [
		"http://742.34.4.178/transmart",
		"http://example.com/transmart"
	]
}}
```

Add to Spring Security chain in Config.groovy :

```groovy
'/horde/**'                   : ["permitAll"],
'/hordeConcepts/**'           : ["permitAll"],
```
