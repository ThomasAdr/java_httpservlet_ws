package persons2;

import java.util.concurrent.ConcurrentMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;
import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.beans.XMLEncoder;
import org.json.JSONObject;
import org.json.XML;

public class PersonsServlet extends HttpServlet {
    private Persons persons; // back-end bean

    // Executed when servlet is first loaded into container.
    // Create a Persons object and set its servletContext
    // property so that the object can do I/O.
    @Override
    public void init() {
	persons = new Persons();
	persons.setServletContext(this.getServletContext());
    }

    // GET /persons
    // GET /persons?id=1
    // If the HTTP Accept header is set to application/json (or an equivalent
    // such as text/x-json), the response is JSON and XML otherwise.
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
	String param = request.getParameter("id");
	Integer key = (param == null) ? null : new Integer(param.trim());

	// Check user preference for XML or JSON by inspecting
	// the HTTP headers for the Accept key.
	boolean json = false;

	String payloadParam = request.getParameter("type");
	if (payloadParam != null && payloadParam.contains("json")) json = true;
	System.out.println(payloadParam);
	System.out.println(json);

	String accept = request.getHeader("accept");
	if (accept != null && accept.contains("json")) json = true;
	
        // If no query string, assume client wants the full list.
        if (key == null) {
	    ConcurrentMap<Integer, Person> map = persons.getMap();

	    // Sort the map's values for readability.
	    Object[] list = map.values().toArray();
	    Arrays.sort(list);

	    String xml = persons.toXML(list);
	    sendResponse(response, xml, json);
	}
	// Otherwise, return the specified Prediction.
	else {
	    Person pers = persons.getMap().get(key);

	    if (pers == null) { // no such Prediction
		String msg = key + " does not map to a person.\n";
		sendResponse(response, persons.toXML(msg), false);
	    }
	    else { // requested Prediction found
		sendResponse(response, persons.toXML(pers), json);
	    }
	}
    }

	// POST /persons
	// HTTP body should contain three keys, one for the name ("name"), one for the surname ("surname") and
	// another for the comment ("comment").
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		String name = request.getParameter("name");
		String surname = request.getParameter("surname");
		String comment = request.getParameter("comment");

		// Are the data to create a new prediction present?
		if (name == null || surname == null || comment == null)
			throw new HTTPException(HttpServletResponse.SC_BAD_REQUEST);

		// Create a Person
		Person p = new Person();
		p.setName(name);
		p.setSurname(surname);
		p.setComment(comment);

		// Save the ID of the newly created Prediction.
		int id = persons.addPerson(p);

		// Generate the confirmation message.
		String msg = "Person " + id + " created.\n";
		persons.addPersonToDB();
		sendResponse(response, persons.toXML(msg), false);
	}

	// DELETE /predictions2?id=1
	@Override
	public void doDelete(HttpServletRequest request, HttpServletResponse response) {
		String param = request.getParameter("id");
		Integer key = (param == null) ? null : new Integer(param.trim());
		// Only one Prediction can be deleted at a time.
		if (key == null)
			throw new HTTPException(HttpServletResponse.SC_BAD_REQUEST);
		try {
			persons.getMap().remove(key);
			String msg = "Person " + key + " removed.\n";
			sendResponse(response, persons.toXML(msg), false);
		}
		catch(Exception e) {
			throw new HTTPException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	// Method Not Allowed
    @Override
    public void doTrace(HttpServletRequest request, HttpServletResponse response) {
        throw new HTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    // Method Not Allowed
    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response) {
        throw new HTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    // Method Not Allowed
    @Override
    public void doOptions(HttpServletRequest request, HttpServletResponse response) {
        throw new HTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    // Send the response payload to the client.
    private void sendResponse(HttpServletResponse response, String payload, boolean json) {
	try {
	    // Convert to JSON?
	    if (json) {
		JSONObject jobt = XML.toJSONObject(payload);
		payload = jobt.toString(3); // 3 is indentation level for nice look
	    }

	    OutputStream out = response.getOutputStream();
	    out.write(payload.getBytes());
	    out.flush();
	}
	catch(Exception e) {
	    throw new HTTPException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}
    }
}     
