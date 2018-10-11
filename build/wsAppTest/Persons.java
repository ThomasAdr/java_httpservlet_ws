package persons2;

import java.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collections;
import java.beans.XMLEncoder; // simple and effective
import javax.servlet.ServletContext;

public class Persons {
    private ConcurrentMap<Integer, Person> personMap;
    private ServletContext sctx;
    private AtomicInteger mapKey;

	//Contructors

    public Persons() {
	personMap = new ConcurrentHashMap<Integer, Person>();
	mapKey = new AtomicInteger();
    }

    //** properties

	//Accessors

	//ServletContext
    // The ServletContext is required to read the data from
    // a text file packaged inside the WAR file
    public void setServletContext(ServletContext sctx) {
	this.sctx = sctx;
    }
    public ServletContext getServletContext() { return this.sctx; }

    //ConcurrentMap
    public void setMap(ConcurrentMap<String, Person> personMap) {
	// not used now
    }
    public ConcurrentMap<Integer, Person> getMap() {
	// Has the ServletContext been set?
	if (getServletContext() == null) return null;      

	// Have the data been read already?(and so populated)
	if (personMap.size() < 1) populate();

	return this.personMap;
    }

    //Encodage

    public String toXML(Object obj) {
	String xml = null;

	try {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    XMLEncoder encoder = new XMLEncoder(out);
	    encoder.writeObject(obj); // serialize to XML
	    encoder.close();
	    xml = out.toString(); // stringify
	}
	catch(Exception e) { }
	return xml;
    }

	//AddPerson

    public int addPerson(Person p) {
	int id = mapKey.incrementAndGet();
	p.setId(id);
	personMap.put(id, p);
	return id;
    }

    //Utility

    //** utility
    private void populate() {
	String filename = "/WEB-INF/data/personsDb.db";
	InputStream in = sctx.getResourceAsStream(filename);

	// Read the data into the array of Persons.
	if (in != null) {
	    try {
		InputStreamReader isr = new InputStreamReader(in);
		BufferedReader reader = new BufferedReader(isr);

		int i = 0;
		String record = null;
		while ((record = reader.readLine()) != null) {
		    String[] parts = record.split("!");
		    Person p = new Person();
		    p.setName(parts[0]);
			p.setSurname(parts[1]);
		    p.setComment(parts[2]);
		    addPerson(p);
		}
	    }
	    catch (IOException e) { }
	}
    }

    public void addPersonToDB() {
		String filename = "/WEB-INF/data/personsDb.db";
		InputStream in = sctx.getResourceAsStream(filename);

		// Add the person to the Database
		if (in != null) {
			try (
					FileWriter fw = new FileWriter(filename, true);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter out = new PrintWriter(bw))

			{
				out.println("the text");
				//more code
				out.println("more text");
				//more code
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}




