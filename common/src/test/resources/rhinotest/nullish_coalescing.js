console.info('Nullish Coalescing Operator Test')

// Should print 10
{
	let a = 10
	let b = 20
	let c = a ?? b
	console.info(c)
}

// Should print 20
{
	let a = null
	let b = 20
	let c = a ?? b
	console.info(c)
}

// Should print 20
{
	let a = undefined
	let b = 20
	let c = a ?? b
	console.info(c)
}

// Should print false
{
	let a = false
	let b = 20
	let c = a ?? b
	console.info(c)
}

// Should print 0
{
	let a = 0
	let b = 20
	let c = a ?? b
	console.info(c)
}

// Should print 10
{
	let a = 10
	let b = null
	let c = a ?? b
	console.info(c)
}