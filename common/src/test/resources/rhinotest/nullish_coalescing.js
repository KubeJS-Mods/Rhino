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

// Should print 10
{
	let a = 10
	let b = null
	let c = a ?? b
	console.info(c)
}