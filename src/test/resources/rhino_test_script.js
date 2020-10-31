console.info('Hello!')
const y = x => x * x
console.info(y(5))
console.info(sqTest(8))

events.listen('test', event => {
  console.info('TEEST: ' + event)
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

const { pow } = newMath

console.info(pow(3, 5))

events.testData({someString: 'abc'})

for (n of events.numberList) {
  console.info('Event numbers: ' + n)
}

console.info(events.dynamicMap.awudh.awdhauwhd);
console.info(events.dynamicMap.awuwadawdawdh.fhhue);