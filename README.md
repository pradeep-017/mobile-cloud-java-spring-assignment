## Coursera - Building Cloud Services with the Java Spring Framework

This assignment is a very basic application
for uploading video to a cloud service and managing the video's metadata and its content.
This covers the core knowledge needed to create much more sophisticated cloud services.
There are four HTTP APIs in this:
 
GET /video
   - Returns the list of videos that have been added to the
     server as JSON.
     
POST /video
   - The video metadata is provided as an application/json request
     body. The JSON generates a valid instance of the 
     Video class when deserialized by Spring's default 
     Jackson library.
   - Returns the JSON representation of the Video object that
     is stored along with any updates to that object made by the server. 
     
POST /video/{id}/data
   - The binary mpeg data for the video is provided in a multipart
     request as a part with the key "data". The id in the path is
     the unique identifier generated by the backend for the
     Video. A client MUST *create* a Video first by sending a POST to /video
     and getting the identifier for the newly created Video object before
     sending a POST to /video/{id}/data. 
   - The endpoint returns a VideoStatus object with state=VideoState.READY
     if the request succeeds and the appropriate HTTP error status otherwise. 
     
GET /video/{id}/data
   - Returns the binary mpeg data (if any) for the video with the given
     identifier. If no mpeg data has been uploaded for the specified video,
     then the server returns a 404 status code.
  
