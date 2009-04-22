#Gnuplot script for plotting basilisk .score files
set title "Weapon"
set xlabel "Total Lexicon Entries"
set ylabel "Correct Lexicon Entries"
set term postscript
set output "weapon-plot.ps"
cd "../../lexicon-scores/mcat-plus/"
plot "weapon.score" every 5 using 2:1 title 'Weapon' with points