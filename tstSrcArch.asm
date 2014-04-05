main 
 !.extern exit, a
labelTst:
 sethi %hi(20000), %r3
 ba main
 or %r0, 15, %r12
 or %r1, %hi(ab45e), %r1
 or %r0, %r2, %r2
 ld %r2, b, %r2
 ba d
 or %r0, %r0, %r3
 or %r3, c, %r3
 ld %r1, %r3, %r9 
 ld %r2, %r0, %r10
 add %r9, %r10, %r11
 st %r11, %r3, %r0
 !call exit
!d .word 4, 3, 5
!e .word 4
 .section data 
a .byte 129
b .word 7
c .word 0
ab45e .word 5
 .section text 
 .section data 
 ba main 
 ba main 
