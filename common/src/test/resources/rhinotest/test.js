<!-- HTML Comment test -->

const testObject = {
	a: -39, b: 2, c: 3439438
}

for (let string of console.testArray) {
	console.info(string)
}

for (let string of console.testList) {
	console.info(string)
}

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