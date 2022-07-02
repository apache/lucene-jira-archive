#
# benchmark 'algorithm'
#
# Informal syntax description:
# ===========================
# - Comments start with '#'
# - Serial sequence enclosed within '{ }'
# - Parallel sequence enclosed within '[ ]'
# - Sequence can also end with '>' in which case child tasks would not store their statistics.
#   This can be useful to avoid exploding stats data, for adding say 1M docs.
#   Example - { AddDoc > : 1000000 - would add milion docs, measure that total, but not save stats for each addDoc.
# - To name a sequence, put '"name"' just after '{' or '['.
# - To repeat sequence tasks N times, add ': N' just after '}' or ']' or '>'.
#   Example -  [ AddDoc ] : 4  - would do 4 addDoc in parallel, spawning 4 threads at once.
#   Example -  [ AddDoc AddDoc ] : 4  - would do 8 addDoc in parallel, spawning 8 threads at once.
#   Example -  { AddDoc } : 30 - would do addDoc 30 times in a row.
#   Example -  { AddDoc AddDoc } : 30 - would do addDoc 60 times in a row.
# - To set a rate (operations per minute) to a sequence, add ': N : R' just after '}' or ']' or '>'.
#   This would specify repetition of N with rate of R operations/min.
#   Example -  [ AddDoc ] : 400 : 200 - would do 400 addDoc in parallel, starting 200 threads a minute.
#   Example -  { AddDoc } : 400 : 200 - would do 400 addDoc serially, waiting before starting next add, 
#   if otherwise rate would exceed 200 adds/min.
# - Report either all statistics or by task name prefix, either sum by task name or not.
#   Example -  ReportPrefix Populate - would report all tasks named with prefix "Populate".
#   example -  ReportAll - would report all tasks.
#   example -  ReportAllSum - would report all tasks aggregated by task name.
# - Clear statsistics for next run by the task - 'ClearStats'
# - Make inputs starts over again by - ResetInputs
#   Useful before starting a new round, to make sure the two rounds see the same inputs.
#

{ "Rounds"

    ResetSystem

    { "Populate"
        CreateIndex
        { "MAddDocs" AddDoc } : 4000
        Optimize
        CloseIndex
    }
    

#    OpenReader
#    { "SearchReuseReader1" Search > : 400
#    CloseReader
#
#    { "SearchNewReader1" Search > : 400
#
#    OpenReader
#    [ "SearchReuseReader2" Search > : 400 : 120000
#    CloseReader
#
#    [ "SearchNewReader2" Search > : 400 : 40000
#
#    ResetInputs

    RepSumByPref MAddDocs
    RepSumByPrefRound MAddDocs
    RepSumByPref AddDoc
    RepSumByPrefRound AddDoc

    NewRound

} : 8

#RepAll
RepSumByName
RepSumByNameRound




