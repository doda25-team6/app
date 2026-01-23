$(document).ready(function () {

	function getSMS() {
		return $("textarea").val().trim()
	}

	function getGuess() {
		return $("input[name='guess']:checked").val().trim()
	}

	function cleanResult() {
		$("#result").removeClass("correct")
		$("#result").removeClass("incorrect")
		$("#result").removeClass("error")
		$("#result").html()
	}

	$("button").click(function (e) {
		e.stopPropagation()
		e.preventDefault()

		var sms = getSMS()
		var guess = getGuess()

		$.ajax({
			type: "POST",
			url: "./",
			data: JSON.stringify({ "sms": sms, "guess": guess }),
			contentType: "application/json",
			dataType: "json",
			success: handleResult,
			error: handleError
		})
	})

	function handleResult(res) {
		var wasRight = res.result == getGuess()

		cleanResult()
		$("#result").addClass(wasRight ? "correct" : "incorrect")
		$("#result").html("The classifier " + (wasRight ? "agrees" : "disagrees"))
		$("#result").show()

		// Display confidence score
		var confidenceContainer = $("#confidence-container")
		var confidenceBar = $("#confidence-bar")
		var confidenceText = $("#confidence-text")

		if (res.confidence !== null && res.confidence !== undefined) {
			var confidencePercent = (res.confidence * 100).toFixed(1)
			confidenceBar.css("width", confidencePercent + "%")
			confidenceText.text(confidencePercent + "%")

			// Color code based on confidence level
			if (res.confidence >= 0.8) {
				confidenceBar.css("background-color", "#4CAF50") // Green = high confidence
			} else if (res.confidence >= 0.6) {
				confidenceBar.css("background-color", "#FFC107") // Yellow = medium confidence
			} else {
				confidenceBar.css("background-color", "#F44336") // Red = low confidence
			}

			confidenceContainer.show()
		} else {
			confidenceContainer.hide()
		}
	}

	function handleError(e) {
		cleanResult()
		$("#result").addClass("error")
		$("#result").html("An error occured (see server log).")
		$("#result").show()
	}

	$("textarea").on('keypress', function (e) {
		$("#result").hide()
	})

	$("input").click(function (e) {
		$("#result").hide()
	})
})