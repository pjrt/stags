
function Test() abort
  tag x
  let l = line('.')
  let c = col('.')

  call assert_equal(5, l)
  call assert_equal(7, c)

  if len(v:errors) == 0
    q
  else
    cq
  endif
endfunction
