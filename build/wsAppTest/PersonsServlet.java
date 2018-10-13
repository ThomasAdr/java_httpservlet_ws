package persons2;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.http.HTTPException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.beans.XMLEncoder;
import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.XML;
import org.json.HTTP;
import java.io.InputStream;
import java.io.IOException;


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
	// Building the JSONString the easy way
		try {
			InputStream inputStream = request.getInputStream();
			if (inputStream != null) {

				// This parses the incoming JSON from the request body.
				String jsonData = new BufferedReader(new InputStreamReader(inputStream)) .lines().collect(Collectors.joining("\n"));

				// JsonObject needs the previously parsed JsonString for his constructor.
				// It Creates a new JSONObject with name/value mappings from the JSON string.
				// A JSONObject kind of looks like a map
				// A JSONObject is an unordered collection of zero or more name/value pairs.
				// It provides unmodifiable map view to the JSON object name/value mappings.

				JSONObject jObj = new JSONObject(jsonData);

				Iterator<String> it = jObj.keys();

				// To loop through the JSONObject, we have to use an Iterator, the same way we can loop through a Map.

				while(it.hasNext())
				{
					String key = it.next(); // get key
					Object o = jObj.get(key); // get value
					System.out.println(key + " : " +  o); // print the key and value
				}

			} else {
				// Do smth
			}
		} catch (IOException ex) {
			// throw ex;
		}

    	/*
		// Other way, with a function used to create the JSONString

		// this parses the incoming JSON from the body.
		JSONObject jObj = new JSONObject(getBody(request));
		Iterator<String> it = jObj.keys();

		while(it.hasNext())
		{
			String key = it.next(); // get key
			Object o = jObj.get(key); // get value
			System.out.println(key + " : " +  o); // print the key and value
		}
		*/
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

	public static String getBody(HttpServletRequest request)  {

		String body = null;
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;

		try {
			InputStream inputStream = request.getInputStream();
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			} else {
				stringBuilder.append("");
			}
		} catch (IOException ex) {
			// throw ex;
			return "";
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ex) {

				}
			}
		}

		body = stringBuilder.toString();
		return body;
	}

}     
