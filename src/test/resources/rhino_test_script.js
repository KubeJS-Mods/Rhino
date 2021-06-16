events.listen('abc', event => {
	console.info('Hello!')
	const y = x => x * x
	console.info(y(5))
	console.info(sqTest(8))

	let someArray = [
		'packagedexcrafting:ender_crafter',
		'prefab:block_compressed_stone',
		'prefab:block_double_compressed_stone',
		'prefab:block_triple_compressed_stone',
		'prefab:block_compressed_dirt',
		'prefab:block_double_compressed_dirt',
		'prefab:block_compressed_obsidian',
		'prefab:block_double_compressed_obsidian',
		'prefab:glass_slab',
		'prefab:glass_stairs',
		'thermal:redprint',
		'thermal:lock',
	]

	someArray.forEach(i => {
		console.infoClass({output: i})
	})

	const numbers = [49, 1, 48, 521]
	numbers.sort()

	console.info(numbers)
	console.info({test: 'abc', d: []})

	function Rectangle(w, h) {
		this.width = w;
		this.height = h;
	}

	Rectangle.prototype.toString = function dogToString() {
		return this.width + 'x' + this.height;
	};

	console.info("Rectangle: ")
	const rect = new Rectangle(10, 30)
	console.info(rect) // java toString
	console.info('' + rect) // js toString

	console.info(events.abc)
	console.info(events.abcd)
	events.abc = 'hello'

	for (n of numbers) {
		console.info('Numbers: ' + n)
	}

	console.info(newMath)
	console.info(newMath.pow)
	console.info(newMath.pow(3, 4))

	const {pow} = newMath

	console.info(pow(3, 5))

	events.testData({someString: 'abc'})

	for (n of events.numberList) {
		console.info('Event numbers: ' + n)
	}

	events.numberList = [4948]

	console.info(events.dynamicMap.awudh.awdhauwhd);
	console.info(events.dynamicMap.awuwadawdawdh.fhhue);

	var a = 30
	var b = 'Hello'
	console.info(`Testing templates! ${a} and ${b}`)

	a = "ba"
	b = "QUX"
	var c = `foo bar
${a + "z"} ${b.toLowerCase()}`
	console.info(c)
	console.info("Shallow equality:")
	console.info(c === "foo bar\r\nbaz qux")

	console.info(console.consoleTest)

	const rect2 = new Rect(2345, 5404, 7)
	console.info(`${rect2.width} : ${rect2.height}`)

	events.testWrapper('rhino:test', 5, 409, 4)
	events.testWrapper2(['rhino:array_test_1', 'rhino:array_test_2'])
	events.testWrapper3([[['a', 'b', 'c']], [['d', 'e', 'f']]])

	events.testWrapper('rhino:test', 5, 409, 4)

	events.setSomeId('some:id1')
	events.someId = 'some:id2'
	events.someIdField = 'some:id3'
	console.info(events.someIdField)

	console.info('String namepace and path test')
	let nsPathTest = 'some:id4'
	console.info(nsPathTest.namespace)
	console.info(nsPathTest.path)
	console.info(nsPathTest.charAt(2))

	let customBlock = new ResourceLocation('kubejs', 'custom_block')
	let eq = customBlock == 'kubejs:custom_block'
	let seq = customBlock === 'kubejs:custom_block'

	console.info(`${customBlock}: ${eq}, ${seq}`)
	console.infoClass(event)

	/*
	console.info(java)
	console.info(java.lang)
	console.info(java.lang.Math)
	console.info(java.lang.Math.PI)
	console.info(java.lang.Math.pow(3.0, 5.0))

	var JMath = java.lang.Math
	console.info(JMath.PI)
	*/
})