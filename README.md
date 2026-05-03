# Group rating app
A Java 21 app made for group to democratically decide the performance of members in % built with Maven

# Usage
**For the organizer:**
 1. Find out your IP adress
 2. You might need to open a port via port forwaring for this to work
 3. Open the server jar file
 4. Input the port you'd like to use
 5. Select the number of members from 3 to 20.
 6. Input the names of each member to be rated
 7. Press okay
 8. Wait for members to send in their ratings (The program automatically calculates the % averages)

**For the members:**
1. Open the client jar
2. Get the IP and port from the organizer
3. Input the values for each members (It has to be a whole integer and numbers need to add up to 100)
4. Press Submit rating

## Code files
 - ClientApp.java: the Client UI
 - Constants.java: Stores defaullt values
 - Main.java: Empty
 - RatingBounds.java: Calculates the minmum and maximum assignable values for the %'s of members
 - ServerApp.java: Logic and Server UI
 - SessionConfig.java: Variables for names and rating bounds
