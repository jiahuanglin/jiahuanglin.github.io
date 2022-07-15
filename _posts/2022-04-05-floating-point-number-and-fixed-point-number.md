---
title: Floating point numbers and fixed point numbers
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-04-05 17:12:00 -0500
categories: [Algorithm & Data Structure]
tags: [number format]
---

How can we represent numbers with a point in computer?

## Fixed point numbers
We know that we can represent 32^2 different numbers with 32 bits, which gives us almost 4 billion distinct numbers. An intuitive idea for representing numbers with a point is to fix the point position. Namely, we use 4 bits to represent integers in decimal (0 to 9), so 32 bits would represent 8 such integers. Then we take the rightmost 2 integers as the fractional part, and fix the point right at that position. In this way, we can represent 100 million real numbers from 0 to 999999.99 in 32 bits.

Such binary representation of point decimal is called **BCD (Binary-Coded Decimal)**. It is most often used in supermarkets and banks where we have decimals up to the cent position.

However, the drawback is clear:
1. Waste of bits. With 32 bits we could represent 4 billion different numbers theoretically, but we can only represent 100 million numbers in BCD
2. There is no way to represent huge numbers and tiny numbers simultaneously in this way. We can't let our computers deal only a small fixed range of numbers.

## Floating point numbers
```math
V = (-1)^s * M * (2)^E
```
where
1. `(-1)^s` denotes the sign bit, when `s=0`, V is positive; when `s=1`, V is negative.
2. M indicates a valid number, greater than or equal to 1, less than 2.
3. `2^E` denotes the exponent bit.

IEEE 754 specifies that for 32-bit floating-point numbers, the highest 1 bit is the sign bit s, followed by the next 8 bits are the exponent E, and the remaining 23 bits are the significant digits M.

For 64-bit floating-point numbers, the highest 1 bit is the sign bit S, followed by the next 11 bits are the exponent E, and the remaining 52 bits are the significant digits M.

Note that IEEE 754 has some special rules for the valid number M and exponent E:

- 1 ≤ M < 2, that is, M can be written in 1.xxxxxxx, where xxxxxxx indicates the fractional part. IEEE 754 stipulates that when M is saved internally in a computer, the first bit of this number is always 1 by default, so it can be rounded off and only the xxxxxxx part after it is saved. For example, when saving 1.01, only 01 is saved, and when it is read, the first 1 is added. The purpose of doing this is to save 1 valid digit. Take 32-bit floating-point number as an example, only 23 bits are left for M. After rounding off the first 1, it is equal to save 24 valid digits.

- E is an unsigned integer. However, we know that E in scientific notation can be negative, so IEEE 754 stipulates that the true value of E must be subtracted by an intermediate number, which is 127 for 8-bit E and 1023 for 11-bit E.
  1. E is not all 0 or not all 1. In this case, the floating point number is expressed using the above rule, i.e., the calculated value of the exponent E is subtracted from 127 (or 1023) to get the true value, and then the valid number M is preceded by the first 1.
  2. E is all 0. In this case, the exponent E of the floating point number is equal to 1-127 (or 1-1023), and M is no longer preceded by the first 1, but reduced to a decimal of 0.xxxxxxx. This is done to represent ±0, and tiny little numbers close to 0.
  3. E is all 1. In this case, if M is all 0, it means ± infinity (positive or negative depending on the sign digit s); if M is not all 0, it means the number is not a number (NaN).

## Bfloat16
The `bfloat16` format was developed by Google Brain, an artificial intelligence research group at Google. It is a truncated (16-bit) version of the 32-bit IEEE 754 single-precision floating-point format (binary32) with the intent of accelerating machine learning and near-sensor computing.

The main difference is that `bfloat16` supports only an 8-bit significand rather than the 24-bit significand of the binary32 format.


### Reference
[Wiki: Computer number format](https://en.wikipedia.org/wiki/Computer_number_format)\
[Wiki: bfloat16](https://en.wikipedia.org/wiki/Bfloat16_floating-point_format)