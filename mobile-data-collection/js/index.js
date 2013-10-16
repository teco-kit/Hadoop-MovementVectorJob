var id = "";
var cur_motion = "0,0,0,0,0,0,0";
var cur_orientation = "0,0,0,0";
var last_time = 0;
var status = "standing";
var destination = "http://129.13.169.226:8080/sensorserver"
var buffer = [];
var timerID=null;
var sendInterval = 5000;
var overlap = 1000;

$(document).ready(function() {
	
	if($.cookie("device_guid")!=undefined)
	{
		id = $.cookie("device_guid");
	} else {
		id = generateGUID();
		$.cookie("device_guid",id);
	}
	$("#label_guid").text(id);

	if (window.DeviceMotionEvent) {
 		$("#label_motion").text("True");
	} else {
		$("#label_motion").text("False");
	}

	if (window.DeviceOrientationEvent) {
 		$("#label_rotation").text("True");
	} else {
		$("#label_rotation").text("False");
	}

	$("#btn_standing").button( "disable" );

	$("#btn_standing").click(function(ev) {
		buffer = [];
		$("#btn_standing").button( "disable" );
		$("#btn_moving").button( "enable" );
		status = "standing";		
	});

	$("#btn_moving").click(function(ev) {
		buffer = [];
		$("#btn_moving").button( "disable" );
		$("#btn_standing").button( "enable" );
		status = "moving";		
	});

	$("#btn_capture").click(function(ev) {
		if($("#btn_capture").prev('.ui-btn-inner').children('.ui-btn-text').text().indexOf("Start")!=-1)
		{
			$("#btn_capture").prev('.ui-btn-inner').children('.ui-btn-text').text("Stop capture");
			if (window.DeviceMotionEvent) {
		 		window.addEventListener("devicemotion",updateMotion, false);
			}

			if (window.DeviceOrientationEvent) {		 		
		 		window.addEventListener("deviceorientation", updateOrientation, false);
			}
			timerID = window.setInterval(sendMessage,sendInterval);
		}else{ 
			$("#btn_capture").prev('.ui-btn-inner').children('.ui-btn-text').text("Start capture");
			window.removeEventListener("devicemotion",updateMotion,false);
			window.removeEventListener("deviceorientation",updateOrientation,false);
			window.clearInterval(timerID);
		}
		
	});
});

function updateMotion (ev) {
	cur_motion = ev.acceleration.x + "," + ev.acceleration.y + "," + ev.acceleration.z + "," + ev.rotationRate.alpha + "," + ev.rotationRate.beta + "," + ev.rotationRate.gamma + "," + ev.interval;
	$("#label_rate").text(ev.interval);
	if(ev.timeStamp)
	{
		last_time = ev.timeStamp;
	} else {
		last_time = new Date().getTime();
	}
	updateOutput();
}

function updateOrientation (ev) {
	cur_orientation = "";
	if(ev.alpha)
	{
		cur_orientation = cur_orientation + ev.alpha;
	} else {
		cur_orientation = "0";
	}

	if(ev.beta)
	{
		cur_orientation = cur_orientation + "," + ev.beta;
	} else {
		cur_orientation = cur_orientation + ",0";
	}

	if(ev.gamma)
	{
		cur_orientation = cur_orientation + "," + ev.gamma;
	} else {
		cur_orientation = cur_orientation + ",0";
	}

	if(ev.timeStamp)
	{
		last_time = ev.timeStamp;
	} else {
		last_time = new Date().getTime();
	}
	updateOutput(); 	
}

function updateOutput() {
	var cur_string = id + "," + status + "," + last_time + "," + cur_motion + "," + cur_orientation;
	var obj = {time:last_time, str:cur_string};
	buffer.push(obj);	
}

function sendMessage () {	
	if(buffer.length>0)
	{
		var bufferstring = buffer[0].str;
		for (var i = 1; i < buffer.length; i++) {
			bufferstring = bufferstring + ";" + buffer[i].str;
		};

		$.ajax({
			url:destination,
			type: "POST",
			contentType:'application/x-www-form-urlencoded',
			data:{data:bufferstring},					
			success: function(msg){
				$("#label_string").text(parseInt(msg) * 5 + "s of data");					
			},
			error: function(xml, msg, err){					
				console.log(xml.statusText);
			}
		});		
		clearOverlap();
	}
}

function clearOverlap() {
	var curTime = new Date().getTime();
	while(buffer.length>0 && buffer[0].time < (curTime - (sendInterval - overlap)) )
	{
		buffer.shift();
	}
}

generateGUID = (typeof(window.crypto) != 'undefined' && 
                typeof(window.crypto.getRandomValues) != 'undefined') ?
    function() {
        // If we have a cryptographically secure PRNG, use that
        // http://stackoverflow.com/questions/6906916/collisions-when-generating-uuids-in-javascript
        var buf = new Uint16Array(8);
        window.crypto.getRandomValues(buf);
        var S4 = function(num) {
            var ret = num.toString(16);
            while(ret.length < 4){
                ret = "0"+ret;
            }
            return ret;
        };
        return (S4(buf[0])+S4(buf[1])+"-"+S4(buf[2])+"-"+S4(buf[3])+"-"+S4(buf[4])+"-"+S4(buf[5])+S4(buf[6])+S4(buf[7]));
    }

    :

    function() {
        // Otherwise, just use Math.random
        // http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript/2117523#2117523
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
            return v.toString(16);
        });
    };