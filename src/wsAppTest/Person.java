package persons2;

import java.io.Serializable;

// An array of Persons is to be serialized
// into an XML or JSON document, which is returned to 
// the consumer on a request. 
public class Person implements Serializable, Comparable<Person> {
    private String name;   // person name
    private String surname;  // person surname
    private String comment;  // his/her comment
    private int id;    // identifier used as lookup-key

    //Contructors

    public Person() { }

    //Accessors

    public void setName(String name) {
	this.name = name;
    }
    public String getName() {
        return this.name;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }
    public String getSurname() {
        return this.surname;
    }

    public void setComment(String comment) {
	this.comment = comment;
    }
    public String getComment() {
	return this.comment;
    }

    public void setId(int id) {
	this.id = id;
    }
    public int getId() {
	return this.id;
    }

    // implementation of Comparable interface
    public int compareTo(Person other) {
        return this.id - other.id;
    }

}