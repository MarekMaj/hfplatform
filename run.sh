#!/bin/bash
mvn clean install

timestamp=$(date +%m.%d-%H:%M)
dir=wyniki_nowe/$timestamp


function threads {
    for threads in 1 2 3 4 5 6
	do
	for i in 1 2 3 4 5
	do
	    mkdir -p $dir/threads/$threads/
        mkdir -p $dir/threads_affinity/$threads/
	    echo "$1"
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
	    echo "$1"
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
	    echo "$1"
	    eval $1
	done
	done
}

function factors {
    for factors in 1 2 4 6 8 10
	do
	for i in 1 2 3 4 5
	do
	    mkdir -p $dir/factors/single/$factors/
	    mkdir -p $dir/factors/stm/$factors/
	    echo "$1"
	    eval $1
	done
	done
}

# jeszcze single thread bez affinity

# TESTY WYKONANE BEZ GENERATORA CZASÓW dla bufora wielkości 128
# STM rożna liczba watków (średnie opóźnienie, 99percentyl, przepustowość) - 6 testów watki
#threads "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dinput.buffer.size=128 -Dstm.threads="\$threads" > $dir/threads/\$threads/\$i"

# TESTY WYKONANE BEZ GENERATORA CZASÓW dla bufora wielkości 128 (affinity)
# STM rożna liczba watków affinity (średnie opóźnienie, 99percentyl, przepustowość) - 6 testów watki
#threads "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dinput.buffer.size=128  -Dstm.threads="\$threads" -Daffinity=true > $dir/threads_affinity/\$threads/\$i"

# TESTY WYKONANE BEZ GENERATORA CZASÓW dla bufora wielkości 128 (affinity, 5wątków)
# STM różna liczba kolizji (średnie opóźnienie, 99percentyl, przepustowość, liczba rollbacków) - 4 testy konta 100, 1000, 10000, 100000
#accounts "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dinput.buffer.size=128  -Daccounts.size="\$accounts" -Daffinity=true > $dir/accounts/\$accounts/\$i"

# STM różna wielkośc bufora (średnie opóźnienie, 99percentyl, przepustowość) - 5 testy bufor 16, 64, 256, 1024, 4+1024
# single różna wielkośc bufora (średnie opóźnienie, 99percentyl, przepustowość) - 5 testy bufor 16, 64, 256, 1024, 4+1024
#buffers "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dinput.buffer.size="\$buffers" -Daffinity=true > $dir/buffers/stm/\$buffers/\$i"
#buffers "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.SingleThreadPublishingApp" -Dinput.buffer.size="\$buffers" -Daffinity=true > $dir/buffers/single/\$buffers/\$i"




# dla najlepszej wielkości bufora (kompromis pomiędzy op a przep) i affinity
#pokaze na wykresie porównanie percentyli z poprzedniego punktu

# dla najlepszej wielkości bufora (kompromis pomiędzy op a przep) i affinity
# STM różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 6 testy współczynnik 1, 2, 4, 6, 8, 10 (+0 z poprzedniego)
# single różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 6 testy współczynnik 1, 2, 4, 6, 8, 10 (+0 z poprzedniego)
#factors "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dop.factor="\$factors" -Daffinity=true > $dir/factors/stm/\$factors/\$i"
#factors "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.SingleThreadPublishingApp" -Dop.factor="\$factors" -Daffinity=true > $dir/factors/single/\$factors/\$i"


function uniform {
    for factors in 0 1 2 4 6 8 10
	do
	for i in 1 2 3 4 5
	do
	    mkdir -p $dir/uniform/factors/single/$factors/
	    mkdir -p $dir/uniform/factors/stm/$factors/
	    echo "$1"
	    eval $1
	done
	done
}

function batch {
    for factors in 0 1 2 4 6 8 10
	do
	for i in 1 2 3 4 5
	do
	    mkdir -p $dir/batch/factors/single/$factors/
	    mkdir -p $dir/batch/factors/stm/$factors/
	    echo "$1"
	    eval $1
	done
	done
}

# TESTY WYKONANE Z GENERATOREM ROZKŁAD JEDNOSTAJNY (tutaj tak naprawde średnia powinna sie zmieniac w zalezności od op.factor wzięta z poprzedniego punktu)
# ale przeciez tu mogę sobie zadań, że zadanie ma przychodzic co 1mikros i narazie niech tak będzie
# STM różny czas średni rozkładu jednorodnego czy różna złożonosc operacji
# ten czas średni musi być dobrze dobrany zgodnie z przepusowoscia
# dobrze byłoby tutaj pokazac percentyle
# na wykresie 50, 90, 99 percentyl i przepustowość dla różnej złożoności

# STM rożna liczba watków (średnie opóźnienie, 99percentyl, przepustowość) - 6 testów watki
threads "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="\$threads" -Dtime.gen="uniform" -Dinput.buffer.size="65536" > $dir/threads/\$threads/\$i"
# STM rożna liczba watków affinity
threads "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="\$threads" -Dtime.gen="uniform" -Dinput.buffer.size="65536" -Daffinity=true > $dir/threads_affinity/\$hreads/\$i"

# STM różna liczba kolizji (średnie opóźnienie, 99percentyl, przepustowość, liczba rollbacków) - 4 testy konta 100, 1000, 10000, 100000
accounts "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Daccounts.size="\$accounts" -Dtime.gen="uniform" -Dinput.buffer.size="65536" -Daffinity=true > $dir/accounts/\$accounts/\$i"


# STM różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 7 testy współczynnik 0, 1, 2, 4, 6, 8, 10
# single różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 7 testy współczynnik 0, 1, 2, 4, 6, 8, 10
uniform "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dop.factor="\$factors" -Dtime.gen="uniform" -Dinput.buffer.size="65536" -Daffinity=true > $dir/uniform/factors/stm/\$factors/\$i"
uniform "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.SingleThreadPublishingApp" -Dop.factor="\$factors" -Dtime.gen="uniform" -Dinput.buffer.size="65536" -Daffinity=true > $dir/uniform/factors/single/\$factors/\$i"

# TESTY WYKONANE Z GENERATOREM ROZKŁAD NORMALNY (1mikros, std dev. 200ns)
# STM różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 7 testy współczynnik 0, 1, 2, 4, 6, 8, 10
# single różny wsp. złożonosci (średnie opóźnienie, 99percentyl, przepustowość) - 7 testy współczynnik 0, 1, 2, 4, 6, 8, 10


# TESTY WYKONANE Z GENERATOREM ROZKŁAD Z BATCHAMI
#przychodzi 1000 co 1ms
batch "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dop.factor="\$factors" -Dtime.gen="batch" -Dinput.buffer.size="65536" -Daffinity=true > $dir/batch/factors/stm/\$factors/\$i"
batch "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.SingleThreadPublishingApp" -Dop.factor="\$factors" -Dtime.gen="batch" -Dinput.buffer.size="65536" -Daffinity=true > $dir/batch/factors/single/\$factors/\$i"
