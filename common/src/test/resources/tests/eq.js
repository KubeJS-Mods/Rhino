events.listen('eq', event => {
	console.info(events.testString)
	console.info(events.testString == 'abc')
	console.info(events.testString === 'abc')
	console.info(events.testString == 'abcd')
	console.info(events.testString === 'abcd')
})