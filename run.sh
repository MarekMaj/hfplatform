#!/bin/bash
source /etc/environment
mvn clean install

function test {
	for i in 1 2 3 4 5
	do
		eval $1
	done
} 

timestamp=$(date +%m.%d-%H:%M)
dir=wyniki_nowe/$timestamp

function factor {
    for factor in 1 2 4 6 8 10
	do
	for i in 1 2 3 4 5
	do
	    eval $1
	done
	done
}

function threads {
    for threads in 1 2 3 4 5 6
	do
	for i in 1 2 3 4 5
	do
	    mkdir -p $dir/threads/$threads/
        mkdir -p $dir/threads_affinity/$threads/
	    echo $1
	    eval $1
	done
	done
}

function accounts {
    for accounts in 100 1000 10000 100000
	do
	for i in 1 2 3 4 5
	do
	    mkdir -p $dir/accounts/$accounts/
	    echo $1
	    eval $1
	done
	done
}

function buffers {
    for buffers in 16 64 256 1024 4096
	do
	for i in 1 2 3 4 5
	do
	    mkdir -p $dir/buffers/single/$buffers/
	    mkdir -p $dir/buffers/stm/$buffers/
	    echo $1
	    eval $1
	done
	done
}

# jeszcze single thread bez affinity

# TESTY WYKONANE BEZ GENERATORA CZASÓW dla bufora wielkości 128
# STM rożna liczba watków (średnie opóźnienie, 99percentyl, przepustowość) - 6 testów watki
threads "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dinput.buffer.size=128 -Dstm.threads="\$threads" > $dir/threads/\$threads/\$i"

# TESTY WYKONANE BEZ GENERATORA CZASÓW dla bufora wielkości 128 (affinity)
# STM rożna liczba watków affinity (średnie opóźnienie, 99percentyl, przepustowość) - 6 testów watki
threads "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dinput.buffer.size=128  -Dstm.threads="\$threads" -Daffinity=true > $dir/threads_affinity/\$threads/\$i"

# TESTY WYKONANE BEZ GENERATORA CZASÓW dla bufora wielkości 128 (affinity, 5wątków)
# STM różna liczba kolizji (średnie opóźnienie, 99percentyl, przepustowość, liczba rollbacków) - 4 testy konta 100, 1000, 10000, 100000
accounts "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dinput.buffer.size=128  -Daccounts.size="\$accounts" -Daffinity=true > $dir/accounts/\$accounts/\$i"

# STM różna wielkośc bufora (średnie opóźnienie, 99percentyl, przepustowość) - 5 testy bufor 16, 64, 256, 1024, 4+1024
# single różna wielkośc bufora (średnie opóźnienie, 99percentyl, przepustowość) - 5 testy bufor 16, 64, 256, 1024, 4+1024
buffers "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dinput.buffer.size="\$buffers" -Daffinity=true > $dir/buffers/stm/\$buffers/\$i"
buffers "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.SingleThreadPublishingApp" -Dinput.buffer.size="\$buffers" -Daffinity=true > $dir/buffers/single/\$buffers/\$i"




# dla najlepszej wielkości bufora (kompromis pomiędzy op a przep) i affinity
#pokaze na wykresie porównanie percentyli z poprzedniego punktu

# dla najlepszej wielkości bufora (kompromis pomiędzy op a przep) i affinity
# STM różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 6 testy współczynnik 1, 2, 4, 6, 8, 10 (+0 z poprzedniego)
# single różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 6 testy współczynnik 1, 2, 4, 6, 8, 10 (+0 z poprzedniego)

# TESTY WYKONANE Z GENERATOREM ROZKŁAD JEDNOSTAJNY (tutaj tak naprawde średnia powinna sie zmieniac w zalezności od op.factor wzięta z poprzedniego punktu)
# ale przeciez tu mogę sobie zadań, że zadanie ma przychodzic co 1mikros i narazie niech tak będzie
# STM różny czas średni rozkładu jednorodnego czy różna złożonosc operacji
# ten czas średni musi być dobrze dobrany zgodnie z przepusowoscia
# dobrze byłoby tutaj pokazac percentyle
# na wykresie 50, 90, 99 percentyl i przepustowość dla różnej złożoności
# STM różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 7 testy współczynnik 0, 1, 2, 4, 6, 8, 10
# single różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 7 testy współczynnik 0, 1, 2, 4, 6, 8, 10

# TESTY WYKONANE Z GENERATOREM ROZKŁAD NORMALNY (1mikros, std dev. 200ns)
# STM różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 7 testy współczynnik 0, 1, 2, 4, 6, 8, 10
# single różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 7 testy współczynnik 0, 1, 2, 4, 6, 8, 10

# TESTY WYKONANE Z GENERATOREM ROZKŁAD Z BATCHAMI



# to moje rozwiazanie obroni sie tylko w przypadku kosztownych operacji (koszt większy niż zarządzanie STM)
# przychodzących często (częściej niż średni czas przetwarzania dla jednego wątku) lub grupowo
# wtedy STM będzie miał większą przepustowość
# w przypadku jedynie dużej liczby operacji (prostych) raczej będzie przegrywał - POKAZAĆ
# w przypadku skomplikowanych operacji (umiarkowanie częstych) będę wygrywal - POKAZAĆ, tutaj drastycznie będzie spadała przepustowśc
# tutaj moze pokazac jakie bedzie opóźnienei jezeli zdarzenia przychodzą z częstotliwościa większą niż średni czas przetwarzania
# w przypadku skomplikowanych operacji (rzadkich) będę przegrywał



# gdzie przyczyna? przeciez rollbacków nie tak dużo, wszystkie operacje nieblokujące...
# mała liczba wątków
# przyczyna: eden GC, około 1 collection trwający średnio ile ms?? raz na 10mln operacji w STM, zero GC w single


#factor "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dop.factor="\$factor""
#compare "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.SingleThreadPublishingApp" -Dop.factor="\$factor" > wyniki/porownanie/single_\$factor\$i"


#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.SingleThreadPublishingApp" > wyniki/single/\$i"

#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="6" > wyniki/stm/6\$i"
#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="5" > wyniki/stm/5\$i"
#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="3" > wyniki/stm/3\$i"
#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="1" > wyniki/stm/1\$i"
#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="2" > wyniki/stm/2\$i"
#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="4" > wyniki/stm/4\$i"
