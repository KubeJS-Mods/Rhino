console.info('Hello!')
const y = x => x * x
console.info(y(5))
console.info(sqTest(8))

events.listen('test', event => {
  console.info('TEEST: ' + event)
})

console.info([49, 1, 48, 521].sort())
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