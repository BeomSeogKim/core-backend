package baekjoon;

import java.util.Scanner;

/*
 *  백준 2750 - 수 정렬하기
 * https:www.acmicpc.net/problem/2750
 * 난이도: 실버 5
 * 풀이 날짜: 2026-02-21
 * 사용 알고리즘: Merge Sort
 */
public class BJ_2750 {

    static int[] arr;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        arr = new int[n];

        for (int i = 0; i < n; i++) {
            arr[i] = sc.nextInt();
        }

        mergeSort(0, n - 1);

        for (int num : arr) {
            System.out.println(num);
        }
    }

    static void mergeSort(int left, int right) {
        // TODO: base case 작성
        if (left == right) return;
        // TODO: mid 계산
        int mid = (left + right) / 2;
        // TODO: 왼쪽, 오른쪽 재귀 호출
        mergeSort(left, mid);
        mergeSort(mid, right);
        // TODO: merge 호출
        merge(left, mid, right);
    }

    static void merge(int left, int mid, int right) {
        // TODO: temp 배열 선언
        int[] temp = new int[right - left + 1];
        // TODO: i, j, k 포인터 초기화
        int i = left, j = mid + 1, k = 0;
        // TODO: 두 포인터 비교하며 temp에 채우기

        // TODO: 남은 원소 처리

        // TODO: temp → arr 복사
    }
}
