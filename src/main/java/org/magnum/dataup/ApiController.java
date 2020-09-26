/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ApiController {

	private static final String DATA_PARAMETER = "data";

	private static final String ID_PARAMETER = "id";

	private static final String VIDEO_SVC_PATH = "/video";
	
	private static final String VIDEO_DATA_PATH = VIDEO_SVC_PATH + "/{id}/data";
	
	private static long id_seq = 0;
	
	private Map<Long, InputStream> videoMap = new HashMap<>();
	
	List<Video> videoList = new ArrayList<>();

	/**
	 * This endpoint in the API returns a list of the videos that have
	 * been added to the server. The Video objects should be returned as
	 * JSON. 
	 * 
	 * To manually test this endpoint, run your server and open this URL in a browser:
	 * http://localhost:8080/video
	 * 
	 * @return
	 */
	@RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return videoList;
	}
	
	/**
	 * This endpoint allows clients to add Video objects by sending POST requests
	 * that have an application/json body containing the Video object information. 
	 * 
	 * @return
	 */
	@RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		
		
		Video video = Video.create().withId(id_seq++).withContentType(v.getContentType())
				.withDuration(v.getDuration()).withSubject(v.getSubject()).withLocation(v.getLocation())
				.withTitle(v.getTitle()).build();
		video.setDataUrl(getDataUrl(video.getId()));
		videoList.add(video);
		return video;
	}
	
	/**
	 * This endpoint allows clients to set the mpeg video data for previously
	 * added Video objects by sending multipart POST requests to the server.
	 * The URL that the POST requests should be sent to includes the ID of the
	 * Video that the data should be associated with (e.g., replace {id} in
	 * the url /video/{id}/data with a valid ID of a video, such as /video/1/data
	 * -- assuming that "1" is a valid ID of a video). 
	 * 
	 * @return
	 */
	@RequestMapping(value = VIDEO_DATA_PATH, method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<VideoStatus> setVideoData(@PathVariable(ID_PARAMETER) long id, @RequestParam(DATA_PARAMETER) MultipartFile videoData, HttpServletResponse response) {
		if(id < 0) {
			return new ResponseEntity<VideoStatus>(HttpStatus.NOT_FOUND);
		}
		Video video = videoList.get(((int)id) );
		if(video==null) {
			
			return new ResponseEntity<VideoStatus>(HttpStatus.NOT_FOUND);
		} else {
			try {
				videoMap.put(id, videoData.getInputStream());
				VideoFileManager.get().saveVideoData(video, videoData.getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new ResponseEntity<VideoStatus>(new VideoStatus(VideoState.READY), HttpStatus.OK);
	};
	
	/**
	 * This endpoint should return the video data that has been associated with
	 * a Video object or a 404 if no video data has been set yet. The URL scheme
	 * is the same as in the method above and assumes that the client knows the ID
	 * of the Video object that it would like to retrieve video data for.
	 * 
	 * This method uses Retrofit's @Streaming annotation to indicate that the
	 * method is going to access a large stream of data (e.g., the mpeg video 
	 * data on the server). The client can access this stream of data by obtaining
	 * an InputStream from the Response as shown below:
	 * 
	 * VideoSvcApi client = ... // use retrofit to create the client
	 * Response response = client.getData(someVideoId);
	 * InputStream videoDataStream = response.getBody().in();
	 * 
	 * @param id
	 * @return
	 * @throws IOException 
	 */
	@RequestMapping(value = VIDEO_DATA_PATH, method = RequestMethod.GET)
    ResponseEntity<OutputStream> getData(@PathVariable(ID_PARAMETER) long id, HttpServletResponse response) throws IOException {
		if(id < 0) {
			return new ResponseEntity<OutputStream>(HttpStatus.NOT_FOUND);
		}
		Video video = videoList.get(((int)id) );
		VideoFileManager filemanager = VideoFileManager.get();
		OutputStream out = response.getOutputStream();
		try {
			if(video==null)
				return new ResponseEntity<OutputStream>(HttpStatus.NOT_FOUND);
			else {
				
				filemanager.copyVideoData(video, out);
				
			}
			return new ResponseEntity<OutputStream>(out, HttpStatus.OK);
		
		}catch (Exception ex) {
			throw ex;
		} finally {
			out.close();
		}
	};
	
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://"+request.getServerName() 
	      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   return base;
	}
	
}
