<!-- HTML Comment test -->

const testObject = {
	a: -39, b: 2, c: 3439438
}

for (let string of console.testArray) {
	console.info(string)
}

let testList = console.testList

for (let string of testList) {
	console.info(string)
}

console.info('init ' + testList.length)
testList.add('abcawidawidaiwdjawd')
console.info('add ' + testList.length)
testList.push('abcawidawidaiwdjawd')
console.info('push ' + testList.length)
console.info('pop ' + testList.pop() + ' ' + testList.length)
console.info('unshift ' + testList.shift() + ' ' + testList.length)
console.info('map ' + testList.concat(['xyz']).reverse().map(e => e.toUpperCase()).join(" | "))

console.info(Object.keys(testObject))
console.info(Object.values(testObject))
console.info(Object.entries(testObject))

for (let [key, value] of Object.entries(testObject)) {
	console.info(`${key} : ${value}`)
}

let scopes2 = () => {
	var scopes = [];
	for (const i of Object.keys(testObject)) {
		console.info(`Iterating ${i}`)
		console.freeze([i])
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
console.theme = 'Dark'