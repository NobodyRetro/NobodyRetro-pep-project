package Controller;

import java.sql.PreparedStatement;
import java.sql.ResultSet; //So I can get returned keys from SQL
import java.sql.SQLException;

import Util.ConnectionUtil; //so I can use ConnectionUtil.java
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.sql.Statement; //So I can create generated keys in the database
import java.util.ArrayList; //So I can use ArrayLists
import java.util.List; //So I can use lists

import javax.xml.transform.Result;

import org.h2.util.json.JSONObject;

import java.sql.Connection;
import Model.Account; //so I can use everything from Account.java
import Model.Message; //So I can use everything in Massage.java

/**
 * TODO: You will need to write your own endpoints and handlers for your controller. The endpoints you will need can be
 * found in readme.md as well as the test cases. You should
 * refer to prior mini-project labs and lecture materials for guidance on how a controller may be built.
 */
public class SocialMediaController {
    /**
     * In order for the test cases to work, you will need to write the endpoints in the startAPI() method, as the test
     * suite must receive a Javalin object from this method.
     * @return a Javalin app object which defines the behavior of the Javalin controller.
     */
    public Javalin startAPI() {
        Javalin app = Javalin.create();
        app.get("example-endpoint", this::exampleHandler);
        
//REGISTER TESTS 
        app.post("/register", this::handleRegister);

//LOGIN TESTS
        app.post("/login", this::loginHandler);

//CREATE MESSAGE TESTS
        app.post("/messages", this::createMessagesHandler);

//DELETE MESSAGE TESTS
        app.delete("/messages/{id}", this::messagesDeleteHandler);

//RETRIEVE USER MESSAGES TESTS
        app.get("accounts/{id}/messages", this::userMessagesHandler);

//RETRIEVE ALL MESSAGES TEST
        app.get("/messages", this::allMessagesHandler);

//RETRIEVE MESSAGES FROM ID TESTS
        app.get("messages/{id}", this::messagesIDHandler);

//UPDATE MESSAGES TESTS
        app.patch("messages/{id}", this::messagesUpdateHandler);

		return app;
    }

    private void handleRegister(Context context) throws SQLException{
//Connect to Account.java, convert the body sent from the client into the account.class
        Account account = context.bodyAsClass(Account.class); 
//Check username !null & password length > 4
        if(account.getUsername().isEmpty() || account.getPassword().length() < 4){
            context.status(400); return;
        }

        Connection handleConnection = ConnectionUtil.getConnection(); 
//prepared statment creates the username, password and key
        PreparedStatement ps = handleConnection.prepareStatement
        ("insert into account (username,password) values (?,?)", 
        Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, account.getUsername());
        ps.setString(2, account.getPassword());
    

        try{
//try to create the account, gets generated keys, gets the number, returns the account then a 200 status
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            account.setAccount_id(rs.getInt(1));
            context.json(account);
            context.status(200); return;

//if it exists then catch and send 400 status
        } catch (SQLException e) { 
            context.status(400); return;
        }
          
    }
//*****************************************************************************************************************************/    

        private void loginHandler(Context context) {
    //Connect to Account.java, convert the body sent from the client into the account.class
            Account account = context.bodyAsClass(Account.class);
            if(account == null) {
                context.status(400);
                return;
            }
    //Create variables to use
            String username = account.getUsername();
            String password = account.getPassword();
            Connection connection = ConnectionUtil.getConnection();
        
            try {
    //Check if the username from the client is in the database
                PreparedStatement ps = connection.prepareStatement("SELECT account_id FROM account WHERE username = ? and password = ?");
                ps.setString(1, username);
                ps.setString(2,password);
                ResultSet resultSet = ps.executeQuery();
        
    //Successful login
                if (resultSet.next()) {
                        int accountID = resultSet.getInt("account_id");
                        account.setAccount_id(accountID);
                        context.json(account);
                        context.status(200);
                        return;
                }
        
    // If username doesn't exist or password is incorrect, return 401 Unauthorized
                context.status(401);
            } catch (SQLException e) {
                e.printStackTrace();
    // Handle the exception if necessary
                context.status(500); 
            }
        }

//*****************************************************************************************************************************/    

    private void createMessagesHandler(Context context){
/* As a user, I should be able to submit a new post on the endpoint POST localhost:8080/messages. 
The request body will contain a JSON representation of a message, which should be persisted to the database, 
but will not contain a message_id.

- The creation of the message will be successful if and only if the message_text is not blank, is under 255 characters, 
and posted_by refers to a real, existing user. If successful, the response body should contain a JSON of the message, 
including its message_id. The response status should be 200, which is the default. The new message should be persisted 
to the database.
- If the creation of the message is not successful, the response status should be 400. (Client error) */

        Message message = context.bodyAsClass(Message.class);
        String messageText = message.getMessage_text();

        if(messageText.isEmpty() || messageText.length() > 254){
            context.status(400);
            return;
        }
        try{
        Connection connection = ConnectionUtil.getConnection();

        PreparedStatement ps = connection.prepareStatement ("INSERT INTO message (message_text,posted_by,time_posted_epoch) values (?,?,?)",
        Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, messageText);
        ps.setInt(2, message.getPosted_by());
        ps.setLong(3, message.getTime_posted_epoch());

        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        rs.next();
        message.setMessage_id(rs.getInt(1));
        
        context.status(200);
        context.json(message);

        } catch (SQLException e){
            e.printStackTrace();
            context.status(400);
        }
    }

 //*****************************************************************************************************************************/    
   
    private void messagesDeleteHandler (Context context) {
/* As a User, I should be able to submit a DELETE request on the endpoint DELETE localhost:8080/messages/{message_id}.

- The deletion of an existing message should remove an existing message from the database. 
If the message existed, the response body should contain the now-deleted message. The response status should be 200, which is the default.
- If the message did not exist, the response status should be 200, but the response body should be empty. 
This is because the DELETE verb is intended to be idempotent, ie, multiple calls to the DELETE endpoint should respond 
with the same type of response. */

        try{
//Grab the id from the URL path sent from the client(testing), parse the string into an int
            String messageIDString = context.pathParam("id");
            int messsageID = Integer.parseInt(messageIDString);

//Connect to DB
            Connection connection = ConnectionUtil.getConnection();

//Get the message we plan to delete so we can return it if the message gets deleted

            PreparedStatement getMessage = connection.prepareStatement("SELECT * FROM message WHERE message_id=?");
            getMessage.setInt(1,messsageID);
            ResultSet resultSet = getMessage.executeQuery();
            
        if(resultSet.next()){    
            int accountID = resultSet.getInt("posted_by");
            String messageText = resultSet.getString("message_text");
            long timestamp = resultSet.getLong("time_posted_epoch");

            Message deletedMessage = new Message(messsageID, accountID, messageText, timestamp);

//Lets assume the message is within the DB and try to delete it
            PreparedStatement deleteMessage = connection.prepareStatement("DELETE FROM message WHERE message_id=?");
            deleteMessage.setInt(1, messsageID);
            int rowsAffected = deleteMessage.executeUpdate();

//If a row was affected it means we deleted it so return the original message and send a 200
            if(rowsAffected == 1){  
                context.status(200);
                context.json(deletedMessage);

            }else{

//If the message isnt there then send 200
                context.status(200);
            }
        }else{
            context.status(200);
        }   
        } catch (SQLException e){
            e.printStackTrace();
            context.status(500);
        }

}

//*****************************************************************************************************************************/    

    private void userMessagesHandler (Context context) {
//Connect to the path and connect to the DB
        int accountId = Integer.parseInt(context.pathParam("id"));
        Connection connection = ConnectionUtil.getConnection();

        try {
/*(testing doesnt require a check to see if the user exists)
            PreparedStatement userCheckPS = connection.prepareStatement("SELECT * FROM users WHERE user_id = ?");
            userCheckPS.setInt(1, accountId);
            ResultSet userCheckResultSet = userCheckPS.executeQuery();
    
//User doesnt exist 
           if (!userCheckResultSet.next()) {
                context.status(404);
                return;
            }*/
    
//User exists, continue
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM message WHERE posted_by = ?");
            ps.setInt(1, accountId);
            ResultSet resultSet = ps.executeQuery();
    
            List<Message> messages = new ArrayList<>();
            while (resultSet.next()) {

//Varibales to use in Message.java
                int messageID = resultSet.getInt("message_id");
                int accountID = resultSet.getInt("posted_by");
                String messageText = resultSet.getString("message_text");
                long timestamp = resultSet.getLong("time_posted_epoch");
//Constructor for the message
                Message message = new Message(messageID, accountID, messageText, timestamp);
                messages.add(message);
            }
    
            context.json(messages);
            context.status(200);
        } catch (SQLException e) {
            e.printStackTrace();
            context.status(500);
        }
    }

//*****************************************************************************************************************************/    

private void allMessagesHandler(Context context){
/*As a user, I should be able to submit a GET request on the endpoint GET localhost:8080/messages.
- The response body should contain a JSON representation of a list containing all messages retrieved from the database. 
It is expected for the list to simply be empty if there are no messages. The response status should always be 200, which 
is the default.*/

    Connection connection = ConnectionUtil.getConnection();

    try {
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM message");
        ResultSet resultSet = ps.executeQuery();

        List<Message> messages = new ArrayList<>();
        while (resultSet.next()) {
            int messageID = resultSet.getInt("message_id");
            int accountID = resultSet.getInt("posted_by");
            String messageText = resultSet.getString("message_text");
            long timestamp = resultSet.getLong("time_posted_epoch");

            Message message = new Message(messageID, accountID, messageText, timestamp);
            messages.add(message);
        }

        context.json(messages);
        context.status(200);
    } catch (SQLException e) {
        e.printStackTrace();
        context.status(500);
    }
}

//*****************************************************************************************************************************/

    private void messagesIDHandler (Context context) {
        try {
//Get the messageID from the URL path
            String messageIDString = context.pathParam("id");
            int messageID = Integer.parseInt(messageIDString);

//Connect and create a PS targeting all messages from the given message id 
            Connection connection = ConnectionUtil.getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM message WHERE message_id = ?");
            ps.setInt(1, messageID);
            ResultSet resultSet = ps.executeQuery();
    
            if (resultSet.next()) {
//Create variables targeting Message.java
                int messageIDResult = resultSet.getInt("message_id");
                int accountID = resultSet.getInt("posted_by");
                String messageText = resultSet.getString("message_text");
                long timestamp = resultSet.getLong("time_posted_epoch");
//Constructor 
                Message message = new Message(messageIDResult, accountID, messageText, timestamp);
    
                context.json(message);
                context.status(200);
            } else {
//Message not found
                context.status(200); 
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            context.status(500);
        }
    }

//*****************************************************************************************************************************/    

    private void messagesUpdateHandler (Context context) {
/*As a user, I should be able to submit a PATCH request on the endpoint PATCH localhost:8080/messages/{message_id}. 
The request body should contain a new message_text values to replace the message identified by message_id. 
The request body can not be guaranteed to contain any other information.

- The update of a message should be successful if and only if the message id already exists and the new 
message_text is not blank and is not over 255 characters. If the update is successful, the response body should 
contain the full updated message (including message_id, posted_by, message_text, and time_posted_epoch), and the 
response status should be 200, which is the default. The message existing on the database should have the 
updated message_text.

- If the update of the message is not successful for any reason, the response status should be 400. (Client error) */

        try {
            String messageIDString = context.pathParam("id");
            int messageID = Integer.parseInt(messageIDString);
    
// Get the new message_text from the request body
            String newMessageText = context.bodyAsClass(Message.class).getMessage_text();
            
// Check if the message_text is provided and not blank and the length isnt over 254
            if (newMessageText.isEmpty() || newMessageText.length() > 254) {
                context.status(400);
                return;
            }
//Connect to the DB and use a prepared statment to update the message text 
            Connection connection = ConnectionUtil.getConnection();
            PreparedStatement ps = connection.prepareStatement("UPDATE message SET message_text = ? WHERE message_id = ?");
            ps.setString(1, newMessageText);
            ps.setInt(2, messageID);

//Execute the statement and return the number of rows updated
            int rowsUpdated = ps.executeUpdate();

//If more than 0 rows were updated it was successful, grab variables from that updated message
            if (rowsUpdated > 0) {
                PreparedStatement ps2 = connection.prepareStatement("SELECT * FROM message WHERE message_id = ?");
                ps2.setInt(1,messageID);
                ResultSet resultSet = ps2.executeQuery();
                
                if(resultSet.next()){
                    int messageIDResult = resultSet.getInt("message_id");
                    int accountID = resultSet.getInt("posted_by");
                    String messageText = resultSet.getString("message_text");
                    long timestamp = resultSet.getLong("time_posted_epoch");

//Create message object with those vairables and turn it into the json object
                    Message updatedMessage = new Message(messageIDResult, accountID, messageText, timestamp);
    
                    context.json(updatedMessage);
                    context.status(200);
                }else{
                    context.status(400);
                }
            } else {
                context.status(400);
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            context.status(400);
        }
    }

    private void prepareStatement(String string) {

    }





    /**
     * This is an example handler for an example endpoint.
     * @param context The Javalin Context object manages information about both the HTTP request and response.
     */
    private void exampleHandler(Context context) {
        context.json("sample text");
    }


}