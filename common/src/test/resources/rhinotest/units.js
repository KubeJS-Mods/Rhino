// simple eq
console.printUnit('5==5.0')
console.printUnit('5 == 5.0')
// functions
console.printUnit('sin($test*10)*5')
console.printUnit('$test<0.5?-30:40')
console.printUnit('($test<0.5?($test2<0.5?1.5:-4):($test3<0.5*-3?1.5:-4))')
console.printUnit('(sin((time() * 1.1)) * (($screenW - 32) / 2))')
// test sub vs negate
console.printUnit('-2')
console.printUnit('2 - 2')
console.printUnit('-2 - 2')
console.printUnit('2 - --2')
console.printUnit('-   (2**7) - (-2)')
// color
console.printUnit('#FF0044')
console.printUnit('#FFFF0044')