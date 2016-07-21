package com.integration;

import java.io.File;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

public class Integration {

	public static void main(String[] args) throws Exception {
		Binding b = new Binding();
		File groovyPath = new File("src/main/groovy/com/integration");
		String[] roots = new String[] { groovyPath.getAbsolutePath() };
		GroovyScriptEngine engine = new GroovyScriptEngine(roots);

		b.setProperty("input", "test");
		engine.run("App.groovy", b);
		System.out.println(b.getVariable("output"));
	}
}
