events.listen('array_type_wrapper_test', event => {
	console.info('Hello!')
	events.testRLArray('abc', ['mod:id1', 'mod:id2'])
	events.testRLArray('abc', 'mod:id3')
})