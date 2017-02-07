console.log('Loading event')

exports.handler = function (event, context) {
	console.log(event);
	if (event === "BOOM") context.blowUp();
	context.done(null, "Hello World");
}