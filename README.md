# AGTools
AGTools é um conjunto de ferramentas baseadas no GephiToolkit 9.2 para obter o grafo de linhagem acadêmica de um vértice.


## Descrição

A **entrada de dados** é um grafo em formato Gephi e o código do vértice que se deseja obter a linhagem. A **saída** é um grafo em formato Gephi do vértice passado como parâmetro, tendo sido aplicado o layout EventGraphLayout.


## Como usar

    java -jar agtools grafo_de_entrada.gephi codigo_numerido_do_vertice

### Exemplo

    java -jar agtools grafo.gephi 33


## Autor

[Rafael J. P. Damaceno](https://rafaelpezzuto.github.io/), doutorando em Ciência da Computação pela Universidade Federal do ABC.

