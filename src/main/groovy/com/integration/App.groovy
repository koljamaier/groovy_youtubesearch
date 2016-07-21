package com.integration

def file = new File("src/main/groovy/com")
def roots = file.absolutePath;

println "Start Search.groovy"

// Groovy-Groovy Binding
def binding = new Binding()
GroovyScriptEngine engine = new GroovyScriptEngine(roots);
def search = engine.run("search/Search.groovy", binding)
def outputSearch = binding.getVariable("bestVideoId")
def download = engine.run("download/Download.groovy", binding)

// Groovy-Java-Binding: The script can access input-variables. The calling Java-Application can access the output
output = "successful download" //${input}