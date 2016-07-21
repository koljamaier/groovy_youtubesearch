package com.download

/*
 * @param: bestVideoId is defined in the binding
 * Usual "get_video_info?&video_id=..." is not enough for copy right music videos from VEVO etc.
 * Therefor the URL gets enriched with tags like el=vevo
 */
def infoUrl = "https://www.youtube.com/get_video_info?&video_id=$bestVideoId&el=vevo&ps=default&eurl=&gl=US&hl=en"

def response = infoUrl.toURL().text
def data = parseToMap(response)
println data
/*
 * "adaptive_fmts" holds necessary information about the
 * quality of the streams and their direct url
 */
if (data["adaptive_fmts"] instanceof String) {
	data["adaptive_fmts"] = listOfQueryStringMaps(data["adaptive_fmts"]);
}

Map bestAudioStream = bestAudioStream(data["adaptive_fmts"])
println bestAudioStream
def title = bestVideoTitle.replaceAll("[^a-zA-Z0-9.-]", " ")

download(bestAudioStream.url, title)
download(thumbnailUrl, "thumbnail.jpg")
def formatToMp3 = "ffmpeg -y -i "+'"'+"${title}"+'"'+" -codec:a libmp3lame -qscale:a 2 "+'"'+"${title}.mp3"+'"'
def setAlbumCover = "ffmpeg -y -i "+'"'+"${title}.mp3"+'"'+" -i thumbnail.jpg -map 0:0 -map 1:0 -c copy -id3v2_version 3 "+'"'+"${title}Cover.mp3"+'"'
executeOnShell(formatToMp3, new File("YOUR MUSIC FOLDER PATH"))
executeOnShell(setAlbumCover, new File("YOUR MUSIC FOLDER PATH"))
// cleanup
executeOnShell("del "+'"'+"${title}"+'"' , new File("YOUR MUSIC FOLDER PATH"))
executeOnShell("del "+'"'+"${title}.mp3"+'"', new File("YOUR MUSIC FOLDER PATH"))
executeOnShell("move "+'"'+"${title}Cover.mp3"+'"'+" "+'"'+"${title}.mp3"+'"', new File("YOUR MUSIC FOLDER PATH"))

/*
 * Returns a Map of valuable data fields by parsing the raw html.
 * Example:
 * data="instream_long=False&midroll_prefetch_size=1"
 * ->
 * result[instream_long] = False
 * result[midroll_prefetch_size] = 1
 */
Map parseToMap(data) {
	def result = [:]
	data.split("&").each{ entry ->
		// Note: The actual values might contain "&" again by using URLDecoder.
		// Ex.: "url=http%3A%2F%2Fwww.youtube.com%2Fapi%2Ftimedtext%3Fkey%3Dyttt1%26signature%3DE7" -> "http://www.youtube.com/api/timedtext?key=yttt1&signature=E7"
		result[URLDecoder.decode(entry.substring(0, entry.indexOf("=")))] =
				URLDecoder.decode(entry.substring(entry.indexOf("=") + 1));
	}
	result
}

/*
 * The single data fields (like "adaptive_fmts") might still be complex and
 * include their own fields separated with ",".
 * This method retrieves the nested data of dataField
 */
def listOfQueryStringMaps(dataField) {
	def result = []
	dataField.split(",").each{ entry ->
		result << parseToMap(entry)
	}
	result
}

/*
 * Returns the best audio Stream object (concerning the bitrate)
 */
Map bestAudioStream(data) {
	data.findAll { entry ->
		entry.type.indexOf("audio") > -1
	}.inject([:]) { maxEntry, listEntry ->
		if(listEntry.bitrate > maxEntry.bitrate) {
			println "Bitrate: $listEntry.bitrate"
			maxEntry = listEntry
		} else {
			println "Max-Bitrate: $listEntry.bitrate"
			return maxEntry
		}
	}
}

def download (url, filename) {
	new URL(url).withInputStream { inputStream ->
		new File("YOUR MUSIC FOLDER PATH/${filename}").withOutputStream {outputStream ->
			// Standard 4096 buffer size
			inputStream.eachByte( 4096 ) { buffer, nBytes ->
				outputStream.write( buffer, 0, nBytes )
			}
		}
	}
}

def executeOnShell(String command, File workingDir) {
	println command
	def process = new ProcessBuilder("CMD", "/C", command)
									  .directory(workingDir)
									  .redirectErrorStream(true)
									  .start()
	process.inputStream.eachLine {println it}
	process.waitFor();
	return process.exitValue()
  }
