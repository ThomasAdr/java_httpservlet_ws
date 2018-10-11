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

    // GET /java_httServlet_ws
    // GET /java_httServlet_ws?id=1
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

	// POST /java_httServlet_ws
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

	// PUT /java_httServlet_ws
	// HTTP body should contain at least two keys: the person's id
	// and either his/her name, surname or comment.
	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response) {
	/* A workaround is necessary for a PUT request because neither Tomcat
	   nor Jetty generates a workable parameter map for this HTTP verb.
	   In other words, there is no map generated when parameters are added
	   the common way to the request. We will have to generate this map ourselves */
		String key = null;
		String comment = null;
		String name = null;
		String surname = null;
		boolean sur = false;
		boolean nam = false;
		boolean com = false;

		/* Let the hack begin. */
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
			String data = br.readLine();
			/* This BufferedReader will containt our request URI */

	    /* To simplify the hack, assume that the PUT request has exactly
	       two parameters: the id and either our surname or name. Assume, further,
	       that the id comes first. From the client side, a hash character
	       # separates the id and the who/what, e.g.,

	          id=33#surname=Jude
	    */

			/* ------------------ */

			/* id=33#surname=Jude#name=Allision#comment=This Is The Best Comment Ever */

			/* Splitting the whole URI to get parameter name + value combo */
			/* Putting them into an array */
			String[] args = data.split("#");      // id in args[0], rest in args[1]
			/* Splitting the parameter name + value combo to get each part individually */
			/* Putting them into an array */
			/* Working with parameter 1 */
			String[] parts1 = args[0].split("="); // id = parts1[1]
			key = parts1[1];
			/* Splitting the parameter name + value combo to get each part individually */
			/* Putting them into an array */
			/* Working with parameter 2 */
			String[] parts2 = args[1].split("="); // parts2[0] is key
			if (parts2[0].contains("surname")) sur = true;
			surname = parts2[1];


		}
		catch(Exception e) {
			throw new HTTPException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		// If no key, then the request is ill formed.
		if (key == null)
			throw new HTTPException(HttpServletResponse.SC_BAD_REQUEST);

		// Look up the specified person.
		Person p = persons.getMap().get(new Integer(key.trim()));
		String msg;
		if (p == null) { // not found?
			msg = key + " does not map to a Person.\n";
			sendResponse(response, persons.toXML(msg), false);
		}
		else { // found
			if (surname == null) {
				throw new HTTPException(HttpServletResponse.SC_BAD_REQUEST);
			}
			// Do the editing.
			else {
				if (sur) p.setSurname(surname);
				// else  p.setWhat(rest);

				msg = "Person " + key + " has been edited.\n";
				sendResponse(response, persons.toXML(msg), false);
			}
		}
	}
  
	// DELETE /java_httServlet_ws?id=1
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