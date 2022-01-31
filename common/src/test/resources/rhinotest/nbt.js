// Compound tag

let compoundTagTest = NBT.compoundTag()

const testObject = {
	a: -39, b: '2', c: 3439438
}

compoundTagTest.merge(testObject);
console.info(compoundTagTest)

// List tag

let listTagTest = NBT.listTag()

listTagTest.push('a')
listTagTest.push('b')
listTagTest.push('c')

console.info(listTagTest)
