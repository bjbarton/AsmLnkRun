main: sub %r1, %r14, %r3 ;set sp
 add %r1, %r3, %r1 ;set fp
 sub %r3, 12, %r3    ;set %r8 for passing args		
 add %r3, %r3, %r3       ;add %r8 to stack for arg
 call Fib
 .include tstSrc2.asm
Vars .equ 7
Fib
 .word 4, 12
 or %r3, Vars, %r2 ;save, fp

 