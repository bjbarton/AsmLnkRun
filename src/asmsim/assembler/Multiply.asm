.begin

! Register usage.
! %r0  = always zero (0).
! %r1  = trap 0 argument - service number.
! %r2  = trap 0 argument - I/O type.
!      = sys_abort status code.
! %r3  = trap 0 argument - I/O data or reference.
! %r1  = trap 1 argument - multiplicand.
!      = trap 1 result   - product.
! %r2  = trap 1 argument - multiplier.
! %r14 = stack pointer (increases downward).
! %r15 = call link.
! %r16 = on trap: old  PC value.
! %r17 = on trap: old nPC value (not used by ARCTools).
! %r18 = on trap: old PSR value.
! %r16 - %r23 = locals used by trap handlers (saved and restored).
! %r30 = frame pointer (params +, locals -).
! %r31 = leaf link.

! Note: low memory with the same indices (* 4) as the register indices will be
! used to save/restore register during trap processing.  Thus the first
! 32 words of low memory are reserved for the saved registers.

! ***** I/O Memory-mapped space.

TBR_BASE        .equ    0xff000000
KBD_TRAP        .equ    TBR_BASE + 0x0150
DIS_TRAP        .equ    TBR_BASE + 0x0160
TBR_ORG         .equ    TBR_BASE + 0x0800
WRT_BUFFER      .equ    TBR_BASE + 0x1000
KBD_ISR         .equ    TBR_BASE + 0x2000
DIS_ISR         .equ    TBR_BASE + 0x3000

CIO_BASE        .equ    0xffff0000
COUT            .equ    0x00
COCTL           .equ    0x04
CIN             .equ    0x08
CICTL           .equ    0x0c

! ***** OS regions.

START           .equ    0x0080
TRAP_ORG        .equ    0x0100

TEXT            .equ    0x0800
DATA            .equ    0x4000

FALSE           .equ    0
TRUE            .equ    1

! ***** Trap codes.

!sys:           trap    0
!smul:          trap    1
!sdiv:          trap    2

! ***** System Services.

!sys_abort      .equ    0                       ! process crash
!sys_exit       .equ    1                       ! return to operating system
!sys_write      .equ    2                       ! console write services
!sys_read       .equ    3                       ! console read services
!sys_malloc     .equ    4                       ! memory allocation
!sys_free       .equ    5                       ! memory deallocation

! ***** Write functions.

!write_flush    .equ    0
!write_hex      .equ    1                       ! from reg
!write_int      .equ    2                       ! from reg
!write_string   .equ    3                       ! from addr(asciz string)
!
!                       ***** Macro Definitions. *****

! ***** Macros for synthetic instructions not implemented in ARCTools.

! Set 32-bit constant into register

.macro          set     value, reg
    .if %hi(value) != 0
                sethi   %hi(value), reg
        .if %lo(value) != 0
                or      reg, %lo(value), reg
        .endif
    .endif
    .if %hi(value) == 0
        .if %lo(value) != 0
                mov     %lo(value), reg
        .endif
        .if %lo(value) == 0
                clr     reg
        .endif
    .endif
.endmacro

! Increment register and set condition code flags.

.macro          inccc   reg
                addcc   reg, 1, reg
.endmacro

! Decrement register and set condition code flags.

.macro          deccc   reg
                subcc   reg, 1, reg
.endmacro

! Jump to address.

.macro          jmp     addr
                set     addr, %r1
                jmpl    %r1, %r0
.endmacro

! Call to leaf subroutine/function.

.macro          calll   addr
                set     addr, %r1
                jmpl    %r1, 0, %r31
.endmacro

! Return from subroutine/function.

.macro          ret
                jmpl    %r15, 4, %r0
.endmacro

! Return from leaf subroutine/function.

.macro          retl
                jmpl    %r31, 4, %r0
.endmacro

! Bit manipulation (btst, bset, bclr, btog).
! bits can be in a register or a signed-immediate-13 value.

.macro          btst    bits, reg
                andcc   reg, bits, %r0
.endmacro

.macro          bset    bits, reg
                or      reg, bits, reg
.endmacro

.macro          bclr    bits, reg
                andn    reg, bits, reg
.endmacro

.macro          btog    bits, reg
                xor     reg, bits, reg
.endmacro

! ***** Macros in support of stack operations.

! Set stack to start at specified address.

.macro          setStack stkAddr
                set   stkAddr, %r14
.endmacro

! Push contents of reg onto stack.

.macro          push    reg
                sub     %r14, 4, %r14
                st      reg, %r14
.endmacro

! Pop top of stack into reg.

.macro          pop     reg
                ld      %r14, reg
                add     %r14, 4, %r14
.endmacro

! Peek top of stack into reg without adjusting the stack pointer.

.macro          peek    reg
                ld      %r14, reg
.endmacro

! Adjust the stack pointer to add/subtract nbytes.
! Negative values expand stack.
! Positive values reduce stack.

.macro          stkadj  nbytes
                add     %r14, nbytes, %r14
.endmacro

! Construct fp link and reserve locals space (nbytes).
! Save old fp (%r30).
! Set new fp (%r30) to current sp (%r14).
! [Adjust sp (%r14) to reserve locals space.]

.macro          setfp   nbytes
                push    %r30
                mov     %r14, %r30
    .if nbytes > 0
                sub     %r14, nbytes, %r14
    .endif
.endmacro

! Restore fp link and remove locals space.
! Set current sp (%r14) to current fp (%r30).
! Restore old fp (%r30).

.macro          resetfp
                mov     %r30, %r14
                pop     %r30
.endmacro

! ***** Macros in support of system services.

! Invoke system service.
! Sets service number in %r1 and invokes trap 0 (System Service).
! For sys_abort, sets reason code in %r2.

.macro          sys_abort reason
                mov     reason, %r2
                set     0, %r1
                ta      0
.endmacro

.macro          sys_exit
                set     1, %r1
                ta      0
.endmacro

.macro          sys_write
                set     2, %r1
                ta      0
.endmacro

.macro          sys_read
                set     3, %r1
                ta      0
.endmacro

! Requests memory allocation/deallocation from the heap service.
! Sets service number in %r1 and invokes trap 0 (System Service).
! Set allocation request size in %r2 (may be a register or simm13),
!   therefore the size is limited to 4095 bytes.
! If no more heap memory, abort with reason 4 (sys_malloc).
! If successful, return address of block allocated in %r1.

.macro          sys_malloc size
                set     4, %r1
                mov     size, %r2
                ta      0
.endmacro

.macro          sys_free
                set     5, %r1
                ta      0
.endmacro

! Request write functions.
! Sets service number in %r1 and invokes trap 0 (System Service).
! Sets write function number in %r2.
! Sets value to write in %r3 (see below);

.macro          write_flush
                set     0, %r2
                sys_write
.endmacro

.macro          write_hex reg
                set     1, %r2
                mov     reg, %r3
                sys_write
.endmacro

.macro          write_int reg
                set     2, %r2
                mov     reg, %r3
                sys_write
.endmacro

.macro          write_string addr
                set     3, %r2
                set     addr, %r3
                sys_write
.endmacro

! ***** Macros in support of unimplemented arithmetic instructions.

! Implement a signed multiply instruction via a system trap (1).
! This macro places the multiplicand in %r1.
! This macro places the multiplier in %r2
! The trap stores the result in %r1 which is copied to the product.

.macro          smul    multiplicand, multiplier, product
                mov     multiplicand, %r1
                mov     multiplier, %r2
                ta      1
                mov     %r1, product
.endmacro

! Implement a signed divide instruction via a system trap (2).
! This macro places the dividend in %r1.
! This macro places the divisor in %r2
! The trap stores the result in %r1 which is copied to the quotient.

.macro          sdiv    dividend, divisor, quotient
                mov     divident, %r1
                mov     divisor, %r2
                ta      1
                mov     %r1, quotient
.endmacro

! ***** Macros in support of unimplemented save/restore instructions.

! Implement save to save local registers during system trap invocation.

.macro          save
                st      %r16, [16 * 4]
                st      %r17, [17 * 4]
                st      %r18, [18 * 4]
                st      %r19, [19 * 4]
                st      %r20, [20 * 4]
                st      %r21, [21 * 4]
                st      %r22, [22 * 4]
                st      %r23, [23 * 4]
.endmacro

! Implement restore to restore local registers during system trap invocation.

.macro          restore
                ld      [16 * 4], %r16
                ld      [17 * 4], %r17
                ld      [18 * 4], %r18
                ld      [19 * 4], %r19
                ld      [20 * 4], %r20
                ld      [21 * 4], %r21
                ld      [22 * 4], %r22
                ld      [23 * 4], %r23
.endmacro
!
!                       ***** Operating System (low memory) *****

! ***** Operating Systems code in first 2048 bytes of memory.

! On sys_abort - jump to memory address 0 and halt!

                .org    0
                halt

! ***** Heap Data.

! The heap is allocated in blocks of multiples of 8 bytes.

! In front of each ACTIVE block is an 8-byte header:
!   HEAP_ACTIVE
!   size of active area in 8-byte units.
! The area itself is addressed by the start of the data area where
! the size of the area is located at (start - 4).

! In front of each FREE block is an 8-byte header:
!   HEAP_FREE
!   size of free area in 8-byte units.
! If the size == 0 then this marks the end of heap memory.

! If a FREE block is converted to an ACTIVE block and only enough space
! is left over for an empty FREE block (size == 0), which would indicate
! the end of heap memory, then that block will be included in the ACTIVE
! block to prevent a spurious end of heap condition.  When there is enough
! room for a non-empty FREE block it will be created from the remainder of
! the old FREE block.

HEAP            .equ    0x80000000
HEAP_FREE       .equ    0xdeadbeef
HEAP_ACTIVE     .equ    0xcafebabe
HEAP_SIZE       .equ    0x00001000              ! 0x1000 = 4096 blocks
                                                !   => 32768 bytes

                .org    HEAP
                HEAP_FREE                       ! single free block
                HEAP_SIZE                       ! of HEAP_SIZE blocks
                .dwb    HEAP_SIZE * 2           ! 1 block = 2 words
                HEAP_FREE                       ! Indicate the end of the heap
                0

                .org    TRAP_ORG
trap00:         sll     %r1, 2, %r1             ! convert index to offset
                add     %r1, svc_funcs, %r1     ! convert offset to address
                cmp     %r1, svc_funcs_end      ! check for undefined service
                bge     svcAbort                !   abort
                ld      [%r1], %r1              ! indirect reference
                jmpl    %r0, %r1, %r0           ! jump to service handler

ret_from_trap:  rett    %r16, 4

! ***** Abort.

svcAbort:       jmpl    %r0, %r0, %r0

! ***** Exit to OS.

svcExit:        halt

! ***** Write to console services.

svcWrite:       sll     %r2, 2, %r2             ! convert index to offset
                add     %r2, wrt_funcs, %r2     ! convert offset to address
                cmp     %r2, wrt_funcs_end      ! check for undefined service
                bge     svcAbort                !   abort
                ld      [%r2], %r2              ! indirect reference
                jmpl    %r0, %r2, %r0           ! jump to service handler

! ***** Write Data.

wrtBufPtr:      WRT_BUFFER

!               "0123456789abcdef"  (binary, octal, decimal, and hex digits).
DIGITS:         0x30313233, 0x34353637, 0x38394142, 0x43444546

DECADES:        1000000000
                 100000000
                  10000000
                   1000000
                    100000
                     10000
                      1000
                       100
                        10
                         1

! ***** Flush write buffer.

wrtFls:         save
                set     CIO_BASE, %r16          ! %r16: Base of KBD/DIS regs
                set     WRT_BUFFER, %r17        ! %r17: current buffer ptr
                ld      [wrtBufPtr], %r18       ! %r18: final buffer ptr
WFloop:         cmp     %r17, %r18              ! check if done
                bpos    WFend
WFwait:         ldsb    [%r16 + COCTL], %r19    ! %r19: display status byte
                tst     %r19
                bpos    WFwait
                ldub    [%r17], %r20            ! %r20: display data byte
                stb     %r20, [%r16 + COUT]     ! store in display data reg
                inc     %r17                    ! next byte
                ba      WFloop

WFend:          set     WRT_BUFFER, %r18        ! reset buffer ptr to start
                st      %r18, [wrtBufPtr]
                restore
                ba      ret_from_trap

! ***** Write register in hex format to write buffer.

wrtHex:         save
                ld      [wrtBufPtr], %r18       ! %r18: current buffer ptr
                set     8, %r16                 ! %r16: loop counter
WHloop:         srl     %r3, 28, %r17           ! %r17: char index
                ldub    [%r17 + DIGITS], %r17   ! %r17: char code
                stb     %r17, %r18              ! store char in buffer
                inc     %r18                    ! incr. buffer ptr
                sll     %r3, 4, %r3             ! shift word to get next nybble
                deccc   %r16                    ! decr. loop counter
                bg      WHloop
                st      %r18, [wrtBufPtr]       ! save current buffer ptr
                restore
                ba      ret_from_trap

! ***** Write register in decimal (int) format to write buffer.

wrtInt:         save
                ld      [wrtBufPtr], %r18       ! %r18: current buffer ptr
                tst     %r3                     ! if (num == 0)
                bne     WInot0                  ! {
                set     0x30, %r16              !   store char ('0')
                stb     %r16, %r18              !     in WRT_BUFFER
                inc     %r18                    !   incr. buffer ptr
                ba      WIret                   !   return;
                                                ! }
WInot0:         bg      WIpos                   ! if (num < 0) {
                neg     %r3, %r3                !   num = -num;
                set     0x2d, %r16              !   store char ('-')
                stb     %r16, %r18              !     in WRT_BUFFER
                inc     %r18                    !   incr. buffer ptr
                                                ! }
WIpos:          set     TRUE, %r20              ! %r20: leading = true;
                set     0, %r16                 ! %r16: i = 0;
WIloop1:        cmp     %r16, 10                ! for (; i < 10; i++)
                bge     WIret                   ! {
                sll     %r16, 2, %r17           !   %r17: offset
                ld      [%r17 + DECADES], %r17  !   %r17: dec = DECADES[i];
                set     0, %r19                 !   %r19: count = 0;
WIloop2:        cmp     %r3, %r17               !   while (num >= dec)
                bl      WIend2                  !   {
                inc     %r19                    !      count++;
                sub     %r3, %r17, %r3          !      num -= dec;
                set     FALSE, %r20             !      leading = false;
                ba      WIloop2                 !   }

WIend2:         tst     %r20                    !   if ( ! leading)
                bne     WIlead                  !   {
                ldub    [%r19 + DIGITS], %r19   !     get char from count
                stb     %r19, %r18              !     store char in WRT_BUFFER
                inc     %r18                    !     incr. buffer ptr
                                                !   }
WIlead:         inc     %r16                    !   incr. i
                ba      WIloop1                 ! }

WIret:          st      %r18, [wrtBufPtr]       ! save buffer ptr
                restore
                ba      ret_from_trap

! ***** Write null-terminated string to write buffer.

wrtStr:         save
                ld      [wrtBufPtr], %r18       ! %r18: current buffer ptr

WSloop:         ldub    [%r3], %r16             ! %r16: input byte
                tst     %r16                    ! check for null byte
                be      WSend
                stb     %r16, %r18              ! store char in buffer
                inc     %r18                    ! incr. buffer ptr
                inc     %r3                     ! incr. input byte ptr
                ba      WSloop

WSend:          st      %r18, [wrtBufPtr]       ! save current buffer ptr
                restore
                ba      ret_from_trap

svcRead:        nop
                ba      ret_from_trap

! ***** Allocate memory from heap.

! %r2: request size in bytes.
! %r1: returns address of start of block.

svcMalloc:      save
                set     HEAP, %r16              ! %r16: HEAP ptr
                set     HEAP_FREE, %r22         ! %r22: HEAP_FREE magic
                set     HEAP_ACTIVE, %r23       ! %r23: HEAP_ACTIVE magic
                add     %r2, 0x7, %r2           ! %r2: round up to mult. of 8
                srl     %r2, 3, %r2             ! %r2: req size in blocks

SMloop:         ld      [%r16 + 0], %r21        ! %r21: magic word
                cmp     %r21, %r22              ! check if FREE block
                be      SMfree                  ! no, may be ACTIVE block
                cmp     %r21, %r23
                bne     SMabort                 ! if not ACTIVE => abort
                ld      [%r16 + 4], %r17        ! %r17: size of active block
SMnext:         sll     %r17, 3, %r17           ! %r17: size of block in bytes
                add     %r16, 8, %r16           ! move ptr over header
                add     %r16, %r17, %r16        ! move ptr over block
                ba      SMloop                  ! try next block

SMabort:        sys_abort %r1                   ! abort, reason: sys_malloc

SMfree:         ld      [%r16 + 4], %r17        ! %r17: size of free block
                tst     %r17                    ! if size == 0 => no free space
                be      SMabort
                cmp     %r2, %r17               ! if req won't fit this block
                bg      SMnext                  ! try next block

SMfits:         st      %r23, [%r16 + 0]        ! mark the block as active
                add     %r16, 8, %r1            ! save address for return
                                                !   will be prefixed by size
                sub     %r17, %r2, %r18         ! %r18: remaining free space
                cmp     %r18, 1                 ! if exact fit or only header
                bl      SMexact
                bg      SMnewFree
                                                !   include with active block
SMonlyHdr:      inc     %r2                     !     add 1 to req block size
SMexact:        st      %r2, [%r16 + 4]         !     store active block size
                ba      SMdone

SMnewFree:      st      %r2, [%r16 + 4]         ! store active block size
                add     %r16, 8, %r16           ! move ptr over header
                sll     %r2, 3, %r17            ! %r17: size of block in bytes
                add     %r16, %r17, %r16        ! move ptr over block
                dec     %r18                    ! remove header from free size
                st      %r22, [%r16 + 0]        ! mark new block as free
                st      %r18, [%r16 + 4]        ! store free block size

SMdone:         restore
                ba      ret_from_trap

svcFree:        nop
                ba      ret_from_trap

! ***** Signed integer multiplication.

! Preconditions:
!   %r1 = multiplicand
!   %r2 = multiplier
! Postconditions:
!   %r1 = product
! Correct results will be returned if and only if the arguments
! will not cause and overflow in the 32 bit product.
! If an overflow occurs the result will be meaningless.
! This is a good basis for pseudorandom number generation.

! %r16 will contain the high part of the MQ.
! %r17 will contain the  low part of the MQ.
! %r18 will contain the low bit of the high word of the MQ.
!   This will be used for shifting.
! %r19 will contain the loop counter (32 -> 0)

trap01:         save
                clr     %r16                    ! Set up MQ before the loop
                mov     %r2, %r17
                mov     32, %r19                ! Set the count for the loop
T1loop:         btst    1, %r17                 ! Test low bit of MQ.low
                be      T1skipAdd
                add     %r16, %r1, %r16         ! Add multiplicand to MQ.high
T1skipAdd:      srl     %r17, 1, %r17           ! Shift MQ.low right one bit
                and     %r16, 1, %r18           ! Get low bit MQ.high
                sll     %r18, 31, %r18          ! Shift the bit to the left
                or      %r17, %r18, %r17        ! Add to MQ.low
                sra     %r16, 1, %r16           ! Shift MQ.high right one bit
                deccc   %r19                    ! Decrement count
                bg      T1loop                  ! Repeat as needed
                mov     %r17, %r1               ! Return MQ.low
                restore
                ba      ret_from_trap

trap02:         nop
                ba      ret_from_trap

! ***** OS Startup code and function branch tables.

                .org    START
main:           setStack TBR_BASE
                set     TEXT, %r16              ! TEXT segment begins user code
                set     0x20, %r18              ! Enable Trap bit set in PSR
                rett    %r16, 0

svc_funcs:      svcAbort
                svcExit
                svcWrite
                svcRead
                svcMalloc
                svcFree
svc_funcs_end:  0

wrt_funcs:      wrtFls
                wrtHex
                wrtInt
                wrtStr
wrt_funcs_end:  0

!
!                       ***** Operating System <I/O> (high memory) *****

! ***** Trap jump table (in high memory).
! We can branch (ba) to I/O ISRs also in high memory.
! But we have to jump (jmpl) to services in low memory.
!   Branch offset is limited to +/- 8 MeBytes.

                .org    KBD_TRAP
                ba      KBD_ISR

                .org    DIS_TRAP
                ba      DIS_ISR

                .org    TBR_BASE + 0x0800
                jmpl    %r0, trap00, %r0

                .org    TBR_BASE + 0x0810
                jmpl    %r0, trap01, %r0

                .org    TBR_BASE + 0x0820
                jmpl    %r0, trap02, %r0

! ***** I/O Buffers.

                .org    WRT_BUFFER
wrt_buffer:     .dwb    1024                    ! 4096 bytes for buffer

! ***** Interrupt Service Routines.

                .org    KBD_ISR
                rett    %r16, 4

                .org    DIS_ISR
                rett    %r16, 4
!
!                       ***** User text and data. *****

                .org    DATA
TIMES:          0x202A2000                          ! " * "
EQUALS:         0x203D2000                          ! " = "
DONE:           0x646f6e65                          ! "done\n"
NL:             0x0A000000                          ! "\n"
LC:             0x3c307800                          ! "<0x"
RC:             0x3e000000                          ! ">"
LB:             0x5b000000                          ! "["
VALUE:          0x76616c75, 0x653a2000              ! "value: "
STAR:           0x2a000000                          ! "*"
PLUS:           0x2b000000                          ! "+"
COMMA:          0x2c200000                          ! ", "
LEFT:           0x6c656674, 0x3a203078, 0x00000000  ! "left: 0x"
RIGHT:          0x72696768, 0x743a2030, 0x78000000  ! "right: 0x"
RB:             0x5d0a0000                          ! "]\n"

! ***** Macros used by test program to simplify coding.

.macro          multiply multiplicand, multiplier
                set     multiplicand, %r8
                set     multiplier, %r9
                smul    %r8, %r9, %r10
.endmacro

.macro          newNode nodep
                sys_malloc 12
                st      %r1, [nodep]
.endmacro

.macro          setN    nodep, value, left, right
                set     nodep, %r19             ! %r19: nodep
                set     value, %r20             ! %r20: value
                set     left,  %r21             ! %r21: left
                set     right, %r22             ! %r22: right
.endmacro

! ***** User program starts at the TEXT segment.

! We have to jump around local leaf subroutines and local data space.

                .org    TEXT
                ba      MAIN
                
! ***** Subroutine write for multiply tests.

write:          write_int    %r8                ! Write multiplicand
                write_string TIMES              ! Write operator
                write_int    %r9                ! Write multiplier
                write_string EQUALS             ! Write equals
                write_int    %r10               ! Write result
                write_string NL                 ! Write newline
                write_flush                     ! Flush write buffer
                jmpl    %r31, 4, %r0            ! Return from leaf function

! ***** Subroutine setNode for Node initialization.

setNode:        ld      [%r19], %r19            ! %r19: nodep
                st      %r20, [%r19 + 0]        ! %r20: value => nodep->value
                tst     %r21                    ! %r21: left (null/ptr)
                be      SNleft
                ld      [%r21], %r21            ! %r21: left (null/nodep)
SNleft:         st      %r21, [%r19 + 4]        ! %r21: left => nodep->left
                tst     %r22                    ! %r22: right (null/ptr)
                be      SNright
                ld      [%r22], %r22            ! %r22: right (null/nodep)
SNright:        st      %r22, [%r19 + 8]        ! %r22: right => nodep->right

                write_string LC
                write_hex    %r19
                write_string RC
                write_string LB
                write_string VALUE
                cmp     %r20, 0x2a
                bne     SNnotStar
                write_string STAR
                ba      SNendValue
SNnotStar:      cmp     %r20, 0x2b
                bne     SNnotPlus
                write_string PLUS
                ba      SNendValue
SNnotPlus:      write_int    %r20
SNendValue:     write_string COMMA
                write_string LEFT
                write_hex    %r21
                write_string COMMA
                write_string RIGHT
                write_hex    %r22
                write_string RB
                write_flush
                jmpl    %r31, 4, %r0            ! Return from leaf function

! The data is needed in the TEXT segment for addressing purposes.

n2p:            0
n3p:            0
n4p:            0
times:          0
plus:           0

MAIN:           nop

                ! ***** Multiply tests. *****
                
                multiply  100,  200
                calll    write
                multiply -100,  200
                calll    write
                multiply  100, -200
                calll    write
                multiply -100, -200
                calll    write
                write_flush                     ! Flush write buffer

                ! ***** Node allocation tests. *****
                
                newNode n2p                     ! Allocate nodes
                newNode n3p
                newNode n4p
                newNode times
                newNode plus

                ! ***** Node initialization tests. *****
                
                setN    n2p,      2,   0,     0 ! And set their values
                calll   setNode
                setN    n3p,      3,   0,     0
                calll   setNode
                setN    n4p,      4,   0,     0
                calll   setNode
                setN    times, 0x2a, n3p,   n4p
                calll   setNode
                setN    plus,  0x2b, n2p, times
                calll   setNode
                
                write_string DONE               ! Write done message
                write_flush
                sys_exit                        ! Return to system

.end
