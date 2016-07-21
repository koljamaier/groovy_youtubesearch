package com.search

import java.awt.event.ItemEvent;
import java.math.RoundingMode
import java.util.List;
import java.util.Map;

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.ResourceId
import com.google.api.services.youtube.model.SearchListResponse
import com.google.api.services.youtube.model.SearchResult
import com.google.api.services.youtube.model.Video
import com.google.api.services.youtube.model.VideoListResponse

def JsonFactory JSON_FACTORY = new JacksonFactory();
def HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
def NUMBER_OF_VIDEOS_RETURNED = 5
def YouTube youtube

try {
	youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, null).setApplicationName("groovy-yt-api").build();
	String queryTerm = getInputQuery();

	YouTube.Search.List search = youtube.search().list("id, snippet");
	YouTube.Videos.List videoResource = youtube.videos().list("snippet,statistics,contentDetails");
	String apiKey = "YOUR API KEY"

	// Set up the search.list query
	// TODO Set up order parameter like wanted (date, rating, relevance, title, viewCount)
	search.setKey(apiKey);
	search.setQ(queryTerm);
	search.setFields("items(id,snippet(title, description))");
	search.setType("video")
	//search.setVideoCategoryId("10") //Music
	search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
	SearchListResponse searchResponse = search.execute();
	List<SearchResult> searchResultList = searchResponse.getItems();

	def vidIds = []
	// Set up the videos.list query
	searchResultList.each { searchResult ->
		vidIds << searchResult.getId().getVideoId()
	}
	videoResource.setKey(apiKey);
	videoResource.setId(vidIds.join(","))
	println vidIds.join(",")
	VideoListResponse videoListResp = videoResource.execute();
	println "------------------------------------ VIDEO CANDIDATES ------------------------------------"
	List<Video> videos = videoListResp.getItems();
	videos.each { video -> prettyPrintVideo(video) }
	println "------------------------------------ BEST CANDIDATE ------------------------------------"
	Video bestVideo = bestVideo(videos)
	prettyPrintVideo(bestVideo)
	println "------------------------------------ BEST CANDIDATE JAVA ------------------------------------"
	prettyPrintVideo(bestVideoId(videos))
	// Set binding variables. They are now accessable in Download.groovy
	bestVideoId = bestVideo.getId()
	bestVideoTitle = bestVideo.getSnippet().getTitle()
	thumbnailUrl = bestVideo.getSnippet().getThumbnails().getHigh().getUrl()
	
} catch (GoogleJsonResponseException e) {
	System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
			+ e.getDetails().getMessage());
} catch (IOException e) {
	System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
} catch (Throwable t) {
	t.printStackTrace();
}

def static String getInputQuery() throws IOException {
	String inputQuery = "";
	System.out.print("Please enter a search term: ");
	BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
	inputQuery = bReader.readLine();
	inputQuery;
}

def static void prettyPrintVideo(Video video) {
	println """ 
	Video Id: $video.id
	Title: $video.snippet.title
	Thumbnail: $video.snippet.thumbnails.high.url
	Tags: $video.snippet.tags
	Likes $video.statistics.likeCount
	Dislikes $video.statistics.dislikeCount
	Views $video.statistics.viewCount
	-------------------------------------------------------------
	"""
}

// TODO: Multiply-Factor if HD is available or not (video.getContentDetails().getDefinition())
// TODO: Divide by null if 0 like and 0 dislikes
Video bestVideo(List<Video> videos) {
	def likes;
	def dislikes;
	def viewCount;
	def ratio;
	Video.metaClass.ratio //= 0G //{new BigDecimal(it)}

	videos.inject(new Video(ratio: 0G)) { bestVid, video ->
		likes = video.statistics.likeCount
		dislikes = video.statistics.dislikeCount
		viewCount = video.statistics.viewCount
		video.ratio = (likes/(likes+dislikes))*viewCount
		println "Ratio: $video.ratio \nVideo Id: $video.id"
		if(video.ratio > bestVid.ratio) {
			bestVid = video
		} else {
			return bestVid
		}
	}
}

private static Video bestVideoId(List<Video> videos) {
	BigInteger likes;
	BigInteger dislikes;
	BigDecimal likesDec;
	BigDecimal dislikesDec;
	BigDecimal ratio;
	BigInteger viewCount;
	BigDecimal viewCountDec;
	BigDecimal maxRatio = BigDecimal.ZERO;
	Video bestVideo;
	
	Iterator<Video> iteratorVideoResults = videos.iterator();
	// TODO: Multiply-Factor if HD is available or not (video.getContentDetails().getDefinition())
	while (iteratorVideoResults.hasNext()) {
		Video video = iteratorVideoResults.next();
		likes = video.getStatistics().getLikeCount();
		dislikes = video.getStatistics().getDislikeCount();
		viewCount = video.getStatistics().getViewCount();
		viewCountDec = new BigDecimal(viewCount);
		likesDec = new BigDecimal(likes);
		dislikesDec = new BigDecimal(dislikes);
		ratio = likesDec.divide(likesDec.add(dislikesDec), 2, RoundingMode.HALF_UP).multiply(viewCountDec);
		println "Ratio: ${ratio}\nVideo Id: ${video.id}"
		
		if (ratio.compareTo(maxRatio) == 1) {
			maxRatio = ratio;
			bestVideo = video;
		}
	}
	return bestVideo;
}
