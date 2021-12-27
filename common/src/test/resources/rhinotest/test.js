/*
let scopeBar = () => {
	const bar = 123;
	{
		const bar = 456;
	}
	{
		const bar = 789;
	}
	return bar
}

const x = scopeBar()

console.info(`Let 1 ${x}`)
for (let x = 0; x < 4; x++) {
	console.info(x)
}

console.info(`Let 2 ${x}`)
for (let x = 0; x < 4; x++) {
	console.info(x)
}

console.info(`Var ${x}`)
for (var x = 0; x < 4; x++) {
	console.info(x)
}

console.info(`Default ${x}`)
for (x = 0; x < 4; x++) {
	console.info(x)
}

<!-- HTML Comment test -->

let scopes2 = () => {
	var scopes = [];
	let testArray = ['a', 'b']
	for (i of testArray) {
		console.info(`Iterating ${i}`)
		scopes.push(function () {
			return i;
		});
	}
	console.info(scopes)
	console.info(scopes[0]())
	console.info(scopes[1]())
	return (scopes[0]() === "a" && scopes[1]() === "b");
}

console.info(scopes2())
 */

const test = ['abc', 'def', 'ghi']

for (x of test) {
	console.info(`Test: ${x}`)
}

console.info(`Var: ${x}`)