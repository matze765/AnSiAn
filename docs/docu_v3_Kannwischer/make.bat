@echo off
set filename=document
pdflatex %filename%.tex
for %%f in (*.aux) do ( 
bibtex %%f
)
makeindex -s %filename%.ist -t %filename%.alg -o %filename%.acr %filename%.acn
makeindex -s %filename%.ist -t %filename%.glg -o %filename%.gls %filename%.glo
makeindex -s %filename%.ist -t %filename%.slg -o %filename%.syi %filename%.syg
pdflatex %filename%.tex
pdflatex %filename%.tex
pdflatex %filename%.tex

del %filename%.acn
del %filename%.acr
del %filename%.alg
del %filename%*.aux
del %filename%*.bbl
del %filename%*.blg
del %filename%.glg
del %filename%.glo
del %filename%.gls
del %filename%.ist
del %filename%.lof
del %filename%.log
del %filename%.lop
del %filename%.lot
del %filename%.slg
del %filename%.syg
del %filename%.syi
del %filename%.toc
del %filename%.url
del %filename%.loalg
del btaux.aux
del btaux.bbl
del btaux.blg
del btbbl.aux
del btbbl.bbl
del btbbl.blg

del chapters\*.aux
:: view pdf after compilation

start %filename%.pdf
